package eu.kanade.tachiyomi.ui.manga

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.core.app.ApplicationProvider
import eu.kanade.domain.FlowPreferenceStore
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.chapter.interactor.GetAvailableScanlators
import eu.kanade.domain.manga.interactor.GetExcludedScanlators
import eu.kanade.domain.manga.interactor.GetPagePreviews
import eu.kanade.domain.manga.interactor.UpdateManga
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.data.download.DownloadCache
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import exh.eh.EHentaiUpdateHelper
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import mihon.domain.source.interactor.UpdateMangaFromRemote
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.domain.chapter.interactor.GetMergedChaptersByMangaId
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.interactor.GetCustomMangaInfo
import tachiyomi.domain.manga.interactor.GetFlatMetadataById
import tachiyomi.domain.manga.interactor.GetMangaWithChapters
import tachiyomi.domain.manga.interactor.GetMergedMangaById
import tachiyomi.domain.manga.interactor.GetMergedReferencesById
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager

/**
 * Builds a [MangaScreenModel] over mockk collaborators and real in-memory preferences. Every
 * interactor the model or its composed parts pull from Injekt is registered in Koin; the ones a
 * test drives are fields here. The composed parts' own interactors come from [MangaParts].
 */
internal class MangaHarness {
    val app: Application = ApplicationProvider.getApplicationContext()
    val store: FlowPreferenceStore = FlowPreferenceStore()
    val libraryPreferences: LibraryPreferences = LibraryPreferences(store)
    val trackPreferences: TrackPreferences = TrackPreferences(store)
    val readerPreferences: ReaderPreferences = ReaderPreferences(store)
    val uiPreferences: UiPreferences = UiPreferences(store)
    val sourcePreferences: SourcePreferences = SourcePreferences(store)
    val queue: MutableStateFlow<List<Download>> = MutableStateFlow(emptyList())
    val statuses: MutableSharedFlow<Download> = MutableSharedFlow()
    val downloadManager: DownloadManager = mockk(relaxed = true) {
        every { queueState } returns queue
        every { statusFlow() } returns statuses
        every { progressFlow() } returns statuses
        every { isChapterDownloaded(any(), any(), any(), any(), any()) } returns false
    }
    val cacheChanges: MutableStateFlow<Unit> = MutableStateFlow(Unit)
    val downloadCache: DownloadCache = mockk { every { changes } returns cacheChanges }
    val mangaFlow: MutableStateFlow<Pair<Manga, List<Chapter>>> = MutableStateFlow(manga() to emptyList())
    val getMangaAndChapters: GetMangaWithChapters = mockk {
        coEvery { subscribe(any(), any()) } returns mangaFlow
        coEvery { awaitManga(any()) } answers { mangaFlow.value.first }
        coEvery { awaitChapters(any(), any()) } answers { mangaFlow.value.second }
    }
    val source: Source = mockk(relaxed = true) {
        every { id } returns 7L
        every { name } returns "Plain"
    }
    val sourceManager: SourceManager = mockk(relaxed = true) { every { getOrStub(any()) } returns source }
    val getMergedChapters: GetMergedChaptersByMangaId = mockk {
        coEvery { subscribe(any(), any(), any()) } returns MutableStateFlow(emptyList())
        coEvery { await(any(), any(), any()) } returns emptyList()
    }
    val getMergedManga: GetMergedMangaById = mockk {
        coEvery { subscribe(any()) } returns MutableStateFlow(emptyList())
        coEvery { await(any()) } returns emptyList()
    }
    val getMergedReferences: GetMergedReferencesById = mockk {
        coEvery { subscribe(any()) } returns MutableStateFlow(emptyList())
        coEvery { await(any()) } returns emptyList()
    }
    val getFlatMetadata: GetFlatMetadataById = mockk {
        every { subscribe(any()) } returns MutableStateFlow(null)
        coEvery { await(any()) } returns null
    }
    val getPagePreviews: GetPagePreviews = mockk()
    val scanlators: MutableStateFlow<Set<String>> = MutableStateFlow(emptySet())
    val getAvailableScanlators: GetAvailableScanlators = mockk {
        every { subscribe(any()) } returns scanlators
        every { subscribeMerge(any()) } returns MutableStateFlow(setOf("merge"))
        coEvery { await(any()) } returns emptySet()
        coEvery { awaitMerge(any()) } returns emptySet()
    }
    val excluded: MutableStateFlow<Set<String>> = MutableStateFlow(emptySet())
    val getExcludedScanlators: GetExcludedScanlators = mockk {
        every { subscribe(any()) } returns excluded
        coEvery { await(any()) } returns emptySet()
    }
    val updateManga: UpdateManga = mockk(relaxed = true)
    val updateMangaFromRemote: UpdateMangaFromRemote = mockk()
    val updateHelper: EHentaiUpdateHelper = mockk()
    val trackerManager: TrackerManager = mockk(relaxed = true)
    val parts: MangaParts = MangaParts()
    val lifecycle: LifecycleRegistry = LifecycleRegistry.createUnsafe(mockk<LifecycleOwner>(relaxed = true))

    fun start(vararg extra: Module) {
        lifecycle.currentState = Lifecycle.State.RESUMED
        stopKoin()
        startKoin {
            modules(
                module {
                    single { libraryPreferences }
                    single { trackPreferences }
                    single { readerPreferences }
                    single { uiPreferences }
                    single { sourcePreferences }
                    single { BasePreferences(app, store) }
                    single<PreferenceStore> { store }
                    single { downloadManager }
                    single { downloadCache }
                    single { getMangaAndChapters }
                    single { sourceManager }
                    single { getMergedChapters }
                    single { getMergedManga }
                    single { getMergedReferences }
                    single { getFlatMetadata }
                    single { getPagePreviews }
                    single { getAvailableScanlators }
                    single { getExcludedScanlators }
                    single { updateManga }
                    single { updateMangaFromRemote }
                    single { updateHelper }
                    single { trackerManager }
                    single { GetCustomMangaInfo(NoCustomInfo) }
                },
                parts.module(),
                *extra,
            )
        }
    }

    /** Scopes first: a model's in-flight IO work still resolves through Koin until it has finished. */
    fun stop() {
        clearVoyagerScopes()
        stopKoin()
    }

    /** A model built with every default resolved through Koin. */
    fun model(mangaId: Long = 1L, fromSource: Boolean = false, smartSearched: Boolean = false): MangaScreenModel =
        MangaScreenModel(
            context = app,
            lifecycle = lifecycle,
            mangaId = mangaId,
            isFromSource = fromSource,
            smartSearched = smartSearched,
        )

    /** A model whose initial load never finishes, so it stays [MangaScreenModel.State.Loading]. */
    fun loading(): MangaScreenModel {
        coEvery { getMangaAndChapters.awaitManga(any()) } coAnswers { awaitCancellation() }
        return model()
    }

    /** A model whose initial load has finished. */
    fun loaded(mangaId: Long = 1L, smartSearched: Boolean = false): MangaScreenModel =
        model(mangaId = mangaId, smartSearched = smartSearched).also { model ->
            model.awaitSuccess { !it.isRefreshingData }
        }
}

/** Waits until the model is in a success state matching [predicate]. */
internal fun MangaScreenModel.awaitSuccess(
    predicate: (MangaScreenModel.State.Success) -> Boolean = { true },
): MangaScreenModel.State.Success {
    eventually { (state.value as? MangaScreenModel.State.Success)?.let(predicate) == true }
    return state.value as MangaScreenModel.State.Success
}
