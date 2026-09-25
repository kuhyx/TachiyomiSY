package eu.kanade.tachiyomi.ui.reader

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.manga.interactor.SetMangaViewerFlags
import eu.kanade.domain.source.interactor.GetIncognitoState
import eu.kanade.domain.sync.SyncPreferences
import eu.kanade.domain.track.interactor.TrackChapter
import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.DownloadProvider
import eu.kanade.tachiyomi.data.download.getQueuedDownloadOrNull
import eu.kanade.tachiyomi.data.saver.ImageSaver
import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import tachiyomi.core.common.storage.UniFileTempFileManager
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.chapter.interactor.GetMergedChaptersByMangaId
import tachiyomi.domain.chapter.interactor.UpdateChapter
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.history.interactor.GetNextChapters
import tachiyomi.domain.history.interactor.UpsertHistory
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.interactor.GetFlatMetadataById
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.interactor.GetMergedMangaById
import tachiyomi.domain.manga.interactor.GetMergedReferencesById
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager

/** The file of the download-queue extensions the view model calls. */
internal const val QUEUE_KT: String = "eu.kanade.tachiyomi.data.download.DownloadManagerQueueKt"

/**
 * Every collaborator of a [ReaderViewModel]: real preferences over one shared store, relaxed mocks
 * for the interactors, and the Koin graph for what the composed helpers pull from Injekt.
 */
internal class ReaderVmHarness {
    val store: MapPreferenceStore = MapPreferenceStore()
    val context: Application = mockk(relaxed = true)
    val readerPreferences: ReaderPreferences = ReaderPreferences(store)
    val basePreferences: BasePreferences = BasePreferences(context, store)
    val downloadPreferences: DownloadPreferences = DownloadPreferences(store)
    val trackPreferences: TrackPreferences = TrackPreferences(store)
    val libraryPreferences: LibraryPreferences = LibraryPreferences(store)
    val syncPreferences: SyncPreferences = SyncPreferences(store)
    val uiPreferences: UiPreferences = UiPreferences(store)
    val sourceManager: SourceManager = mockk(relaxed = true)
    val downloadManager: DownloadManager = mockk(relaxed = true)
    val downloadProvider: DownloadProvider = mockk(relaxed = true)
    val tempFileManager: UniFileTempFileManager = mockk(relaxed = true)
    val getManga: GetManga = mockk(relaxed = true)
    val getChaptersByMangaId: GetChaptersByMangaId = mockk(relaxed = true)
    val getNextChapters: GetNextChapters = mockk(relaxed = true)
    val updateChapter: UpdateChapter = mockk(relaxed = true)
    val setMangaViewerFlags: SetMangaViewerFlags = mockk(relaxed = true)
    val getIncognitoState: GetIncognitoState = mockk<GetIncognitoState>().also {
        every { it.await(any()) } returns false
    }
    val getFlatMetadataById: GetFlatMetadataById = mockk(relaxed = true)
    val getMergedMangaById: GetMergedMangaById = mockk(relaxed = true)
    val getMergedReferencesById: GetMergedReferencesById = mockk(relaxed = true)
    val getMergedChaptersByMangaId: GetMergedChaptersByMangaId = mockk(relaxed = true)
    val imageSaver: ImageSaver = mockk(relaxed = true)
    val upsertHistory: UpsertHistory = mockk(relaxed = true)
    val trackChapter: TrackChapter = mockk(relaxed = true)

    val manga: Manga = Manga.create().copy(id = 10L, source = 1L, ogTitle = "Manga")

    /** The chapters [GetChaptersByMangaId] serves for [manga]. */
    fun chapters(vararg chapters: Chapter) {
        coEvery { getChaptersByMangaId.await(manga.id, any()) } returns chapters.toList()
    }

    private val graph: Module = module {
        single { context }
        single { imageSaver }
        single { upsertHistory }
        single { trackChapter }
        single { updateChapter }
        single { sourceManager }
        single { downloadManager }
        single { downloadProvider }
        single { tempFileManager }
        single { readerPreferences }
        single { basePreferences }
        single { downloadPreferences }
        single { trackPreferences }
        single { getManga }
        single { getChaptersByMangaId }
        single { getNextChapters }
        single { setMangaViewerFlags }
        single { getIncognitoState }
        single { libraryPreferences }
        single { syncPreferences }
        single { uiPreferences }
        single { getFlatMetadataById }
        single { getMergedMangaById }
        single { getMergedReferencesById }
        single { getMergedChaptersByMangaId }
    }

    fun start(vararg extra: Module) {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        mockkStatic(QUEUE_KT)
        every { downloadManager.getQueuedDownloadOrNull(any()) } returns null
        startKoin { modules(graph, *extra) }
    }

    fun stop() {
        unmockkAll()
        stopKoin()
        Dispatchers.resetMain()
    }

    fun viewModel(savedState: SavedStateHandle = SavedStateHandle()): ReaderViewModel = ReaderViewModel(
        savedState = savedState,
        sourceManager = sourceManager,
        downloadManager = downloadManager,
        downloadProvider = downloadProvider,
        tempFileManager = tempFileManager,
        readerPreferences = readerPreferences,
        basePreferences = basePreferences,
        downloadPreferences = downloadPreferences,
        trackPreferences = trackPreferences,
        getManga = getManga,
        getChaptersByMangaId = getChaptersByMangaId,
        getNextChapters = getNextChapters,
        updateChapter = updateChapter,
        setMangaViewerFlags = setMangaViewerFlags,
        getIncognitoState = getIncognitoState,
        libraryPreferences = libraryPreferences,
        syncPreferences = syncPreferences,
        uiPreferences = uiPreferences,
        getFlatMetadataById = getFlatMetadataById,
        getMergedMangaById = getMergedMangaById,
        getMergedReferencesById = getMergedReferencesById,
        getMergedChaptersByMangaId = getMergedChaptersByMangaId,
    )

    /** A view model whose state already holds [manga] and whose chapter id is [chapterId]. */
    fun loadedViewModel(chapterId: Long = 1L): ReaderViewModel = viewModel().also { vm ->
        vm.updateState { it.copy(manga = manga) }
        vm.chapterId = chapterId
    }
}

/** A domain chapter of [ReaderVmHarness.manga]. */
internal fun domainChapter(
    id: Long,
    number: Double = id.toDouble(),
    read: Boolean = false,
    bookmark: Boolean = false,
    sourceOrder: Long = 100 - id,
): Chapter = Chapter.create().copy(
    id = id,
    mangaId = 10L,
    url = "/chapter/$id",
    name = "Chapter $id",
    chapterNumber = number,
    read = read,
    bookmark = bookmark,
    sourceOrder = sourceOrder,
)
