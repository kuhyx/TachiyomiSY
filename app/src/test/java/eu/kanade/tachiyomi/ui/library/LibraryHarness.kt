package eu.kanade.tachiyomi.ui.library

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import eu.kanade.domain.FlowPreferenceStore
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.chapter.interactor.SetReadStatus
import eu.kanade.domain.manga.interactor.UpdateManga
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.sync.SyncPreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.download.DownloadCache
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.track.BaseTracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.source.online.installSilentXLog
import eu.kanade.tachiyomi.ui.base.customInfoModule
import eu.kanade.tachiyomi.ui.manga.clearVoyagerScopes
import eu.kanade.tachiyomi.ui.manga.eventually
import exh.search.SearchEngine
import exh.source.ExhPreferences
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.interactor.SetDisplayMode
import tachiyomi.domain.category.interactor.SetMangaCategories
import tachiyomi.domain.category.interactor.SetSortModeForCategory
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.chapter.interactor.GetBookmarkedChaptersByMangaId
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.chapter.interactor.GetMergedChaptersByMangaId
import tachiyomi.domain.history.interactor.GetNextChapters
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.interactor.GetIdsOfFavoriteMangaWithMetadata
import tachiyomi.domain.manga.interactor.GetLibraryManga
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.interactor.GetMergedMangaById
import tachiyomi.domain.manga.interactor.GetSearchTags
import tachiyomi.domain.manga.interactor.GetSearchTitles
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.interactor.SetCustomMangaInfo
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.interactor.GetTracksPerManga
import tachiyomi.domain.track.model.Track

/**
 * The collaborators of [LibraryScreenModel] and its composed parts: mockk interactors over state
 * flows the tests drive, real in-memory preferences. [start] registers all of them in Koin, so a
 * model built with no arguments resolves every default.
 */
internal class LibraryHarness {
    val app: Application = ApplicationProvider.getApplicationContext()
    val store: FlowPreferenceStore = FlowPreferenceStore()
    val libraryPreferences: LibraryPreferences = LibraryPreferences(store)
    val basePreferences: BasePreferences = BasePreferences(app, store)
    val sourcePreferences: SourcePreferences = SourcePreferences(store)
    val exhPreferences: ExhPreferences = ExhPreferences(store)
    val syncPreferences: SyncPreferences = SyncPreferences(store)
    val libraryManga: MutableStateFlow<List<LibraryManga>> = MutableStateFlow(emptyList())
    val getLibraryManga: GetLibraryManga = mockk {
        every { subscribe() } returns libraryManga
        coEvery { await() } answers { libraryManga.value }
    }
    val categories: MutableStateFlow<List<Category>> = MutableStateFlow(emptyList())
    val getCategories: GetCategories = mockk {
        every { subscribe() } returns categories
        coEvery { await(any()) } returns emptyList()
    }
    val tracks: MutableStateFlow<Map<Long, List<Track>>> = MutableStateFlow(emptyMap())
    val getTracksPerManga: GetTracksPerManga = mockk { every { subscribe() } returns tracks }
    val getNextChapters: GetNextChapters = mockk { coEvery { await(any<Long>(), any<Boolean>()) } returns emptyList() }
    val getChapters: GetChaptersByMangaId = mockk { coEvery { await(any(), any()) } returns emptyList() }
    val getMergedChapters: GetMergedChaptersByMangaId = mockk {
        coEvery { await(any(), any(), any()) } returns emptyList()
    }
    val setReadStatus: SetReadStatus = mockk(relaxed = true)
    val updateManga: UpdateManga = mockk(relaxed = true)
    val setMangaCategories: SetMangaCategories = mockk(relaxed = true)
    val coverCache: CoverCache = mockk(relaxed = true)
    val sourceManager: SourceManager = mockk(relaxed = true)
    val queue: MutableStateFlow<List<Download>> = MutableStateFlow(emptyList())
    val downloadManager: DownloadManager = mockk(relaxed = true) { every { queueState } returns queue }
    val cacheChanges: MutableStateFlow<Unit> = MutableStateFlow(Unit)
    val downloadCache: DownloadCache = mockk { every { changes } returns cacheChanges }
    val loggedIn: MutableStateFlow<List<BaseTracker>> = MutableStateFlow(emptyList())
    val trackerManager: TrackerManager = mockk(relaxed = true) {
        every { loggedInTrackersFlow() } returns loggedIn
        every { loggedInTrackers() } answers { loggedIn.value }
    }
    val getMergedManga: GetMergedMangaById = mockk { coEvery { await(any()) } returns emptyList() }
    val setCustomMangaInfo: SetCustomMangaInfo = mockk(relaxed = true)
    val getTracks: GetTracks = mockk { coEvery { await() } returns emptyList() }
    val getBookmarked: GetBookmarkedChaptersByMangaId = mockk { coEvery { await(any()) } returns emptyList() }
    val getIds: GetIdsOfFavoriteMangaWithMetadata = mockk { coEvery { await() } returns emptyList() }
    val getSearchTags: GetSearchTags = mockk { coEvery { await(any()) } returns emptyList() }
    val getSearchTitles: GetSearchTitles = mockk { coEvery { await(any()) } returns emptyList() }
    val setSortMode: SetSortModeForCategory = mockk(relaxed = true)

    fun start(vararg extra: Module) {
        installSilentXLog()
        stopKoin()
        startKoin { modules(listOf(customInfoModule(), module(), parts()) + extra) }
    }

    /** Scopes first: a model's in-flight IO work still resolves through Koin until it has finished. */
    fun stop() {
        clearVoyagerScopes()
        stopKoin()
    }

    private fun module(): Module = module {
        single { getLibraryManga }
        single { getCategories }
        single { getTracksPerManga }
        single { getNextChapters }
        single { getChapters }
        single { setReadStatus }
        single { updateManga }
        single { setMangaCategories }
        single { basePreferences }
        single { libraryPreferences }
        single { coverCache }
        single { sourceManager }
        single { downloadManager }
        single { downloadCache }
        single { trackerManager }
        single { exhPreferences }
        single { sourcePreferences }
        single { getMergedManga }
        single { setCustomMangaInfo }
        single { getMergedChapters }
        single { syncPreferences }
    }

    // What the composed parts, the helpers and the settings model pull besides the model's own defaults.
    private fun parts(): Module = module {
        single { app }
        single<PreferenceStore> { store }
        single { UiPreferences(store) }
        single { getTracks }
        single { getBookmarked }
        single { SearchEngine() }
        single { getIds }
        single { getSearchTags }
        single { getSearchTitles }
        single { SetDisplayMode(libraryPreferences) }
        single { setSortMode }
        single<NetworkToLocalManga> { mockk() }
        single<GetManga> { mockk() }
    }

    /** A model built with every default resolved through Koin. */
    fun model(): LibraryScreenModel = LibraryScreenModel()

    /** A model whose library holds [entries], once they have reached its state. */
    fun loaded(entries: List<LibraryManga>): LibraryScreenModel {
        libraryManga.value = entries
        val model = model()
        model.await { !it.isLoading && it.libraryData.favorites.size == entries.size }
        return model
    }

    /** A model over [entries] (all in the first category) with every one of them selected. */
    fun selecting(vararg entries: LibraryManga): LibraryScreenModel {
        val model = loaded(entries.toList())
        model.await { state -> state.activeCategory?.let { state.getItemsForCategory(it).size } == entries.size }
        model.selectAll()
        model.await { it.selection.size == entries.size }
        return model
    }
}

/** Waits until the state matches [predicate] and returns it. */
internal fun LibraryScreenModel.await(
    predicate: (LibraryScreenModel.State) -> Boolean,
): LibraryScreenModel.State {
    eventually { predicate(state.value) }
    return state.value
}

/** How long a test waits for work the model or a composition runs on its own. */
internal const val WAIT = 5_000L
