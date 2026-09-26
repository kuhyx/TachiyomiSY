package eu.kanade.tachiyomi.ui.library

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.chapter.interactor.SetReadStatus
import eu.kanade.domain.manga.interactor.UpdateManga
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.sync.SyncPreferences
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.download.DownloadCache
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.track.BaseTracker
import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.ui.base.await
import eu.kanade.tachiyomi.ui.base.customInfoModule
import eu.kanade.tachiyomi.ui.library.LibraryScreenModel.LibraryData
import exh.search.SearchEngine
import exh.source.ExhPreferences
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.core.module.Module
import org.koin.dsl.module
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.interactor.SetMangaCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.chapter.interactor.GetBookmarkedChaptersByMangaId
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.chapter.interactor.GetMergedChaptersByMangaId
import tachiyomi.domain.history.interactor.GetNextChapters
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.interactor.GetIdsOfFavoriteMangaWithMetadata
import tachiyomi.domain.manga.interactor.GetLibraryManga
import tachiyomi.domain.manga.interactor.GetMergedMangaById
import tachiyomi.domain.manga.interactor.GetSearchTags
import tachiyomi.domain.manga.interactor.GetSearchTitles
import tachiyomi.domain.manga.interactor.SetCustomMangaInfo
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.interactor.GetTracksPerManga
import tachiyomi.domain.track.model.Track

/**
 * Everything [LibraryScreenModel] and the pipeline, search and download parts it builds pull from
 * Injekt: real preferences over one store, mockk interactors, and flows the test drives.
 */
internal class LibraryHarness {
    val application: Application = ApplicationProvider.getApplicationContext()
    val store: MapPreferenceStore = MapPreferenceStore()
    val basePreferences: BasePreferences = BasePreferences(application, store)
    val libraryPreferences: LibraryPreferences = LibraryPreferences(store)
    val exhPreferences: ExhPreferences = ExhPreferences(store)
    val sourcePreferences: SourcePreferences = SourcePreferences(store)
    val syncPreferences: SyncPreferences = SyncPreferences(store)

    val library: MutableStateFlow<List<LibraryManga>> = MutableStateFlow(emptyList())
    val categories: MutableStateFlow<List<Category>> = MutableStateFlow(emptyList())
    val tracks: MutableStateFlow<Map<Long, List<Track>>> = MutableStateFlow(emptyMap())
    val loggedIn: MutableStateFlow<List<BaseTracker>> = MutableStateFlow(emptyList())

    val getLibraryManga: GetLibraryManga = mockk { every { subscribe() } returns library }
    val getCategories: GetCategories = mockk { every { subscribe() } returns categories }
    val getTracksPerManga: GetTracksPerManga = mockk { every { subscribe() } returns tracks }
    val getNextChapters: GetNextChapters = mockk()
    val getChaptersByMangaId: GetChaptersByMangaId = mockk()
    val setReadStatus: SetReadStatus = mockk(relaxed = true)
    val updateManga: UpdateManga = mockk(relaxed = true)
    val setMangaCategories: SetMangaCategories = mockk(relaxed = true)
    val coverCache: CoverCache = mockk(relaxed = true)
    val sourceManager: SourceManager = mockk(relaxed = true)
    val downloadManager: DownloadManager = mockk(relaxed = true)
    val downloadCache: DownloadCache = mockk { every { changes } returns MutableStateFlow(Unit) }
    val trackerManager: TrackerManager = mockk(relaxed = true) {
        every { loggedInTrackersFlow() } returns loggedIn
    }
    val getMergedMangaById: GetMergedMangaById = mockk()
    val setCustomMangaInfo: SetCustomMangaInfo = mockk(relaxed = true)
    val getMergedChaptersByMangaId: GetMergedChaptersByMangaId = mockk()
    val getTracks: GetTracks = mockk()
    val getIdsWithMetadata: GetIdsOfFavoriteMangaWithMetadata = mockk()
    val getSearchTags: GetSearchTags = mockk()
    val getSearchTitles: GetSearchTitles = mockk()
    val getBookmarked: GetBookmarkedChaptersByMangaId = mockk()

    /** A model over this harness; call after Koin is started with [koinModules]. */
    fun model(): LibraryScreenModel = LibraryScreenModel()

    /** A loaded model whose favourites are [mangas], all of them selected. */
    fun selectedModel(vararg mangas: Manga): LibraryScreenModel {
        val model = model()
        model.state.await { !it.isLoading }
        val items = mangas.map { libraryItem(it) }
        model.updateState {
            it.copy(
                libraryData = LibraryData(isInitialized = true, favorites = items),
                selection = items.map(LibraryItem::id).toSet(),
            )
        }
        return model
    }

    fun koinModules(): List<Module> = listOf(
        customInfoModule(),
        module {
            single { application }
            single { basePreferences }
            single { libraryPreferences }
            single { exhPreferences }
            single { sourcePreferences }
            single { syncPreferences }
            single { getLibraryManga }
            single { getCategories }
            single { getTracksPerManga }
            single { getNextChapters }
            single { getChaptersByMangaId }
            single { setReadStatus }
            single { updateManga }
            single { setMangaCategories }
            single { coverCache }
            single { sourceManager }
            single { downloadManager }
            single { downloadCache }
            single { trackerManager }
            single { getMergedMangaById }
            single { setCustomMangaInfo }
            single { getMergedChaptersByMangaId }
            single { getTracks }
            single { SearchEngine() }
            single { getIdsWithMetadata }
            single { getSearchTags }
            single { getSearchTitles }
            single { getBookmarked }
        },
    )
}
