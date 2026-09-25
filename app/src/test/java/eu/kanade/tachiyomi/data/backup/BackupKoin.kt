package eu.kanade.tachiyomi.data.backup

import app.cash.sqldelight.SuspendingTransactionWithoutReturn
import eu.kanade.domain.FlowPreferenceStore
import eu.kanade.domain.manga.interactor.UpdateManga
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.data.backup.models.BackupSavedSearch
import eu.kanade.tachiyomi.data.download.DownloadCache
import eu.kanade.tachiyomi.data.track.TrackerManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.protobuf.ProtoBuf
import mihon.domain.extension.interactor.GetExtensionStores
import org.koin.core.module.Module
import org.koin.dsl.module
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.data.CategoriesQueries
import tachiyomi.data.ChaptersQueries
import tachiyomi.data.Database
import tachiyomi.data.MangasQueries
import tachiyomi.data.Saved_searchQueries
import tachiyomi.domain.backup.service.BackupPreferences
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.history.interactor.GetHistory
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.interactor.FetchInterval
import tachiyomi.domain.manga.interactor.GetCustomMangaInfo
import tachiyomi.domain.manga.interactor.GetFavorites
import tachiyomi.domain.manga.interactor.GetFlatMetadataById
import tachiyomi.domain.manga.interactor.GetMangaByUrlAndSourceId
import tachiyomi.domain.manga.interactor.GetMergedManga
import tachiyomi.domain.manga.interactor.InsertFlatMetadata
import tachiyomi.domain.manga.interactor.SetCustomMangaInfo
import tachiyomi.domain.manga.model.CustomMangaInfo
import tachiyomi.domain.manga.repository.CustomMangaRepository
import tachiyomi.domain.manga.repository.MangaRepository
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.storage.service.StorageManager
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.interactor.InsertTrack

/** No entry has custom info; a real [GetCustomMangaInfo] over it serves `Manga`'s companion lookup. */
internal object NoCustomInfo : CustomMangaRepository {
    override fun get(mangaId: Long): CustomMangaInfo? = null

    override fun set(mangaInfo: CustomMangaInfo) = Unit
}

/**
 * Everything the backup creator, the restorers and the jobs pull from Injekt: a mocked [Database]
 * whose query sets answer empty until a test stubs them, real preferences over [store], and every
 * interactor as a mock that answers "nothing there".
 */
internal class BackupKoin(val store: PreferenceStore = FlowPreferenceStore()) {
    val mangas: MangasQueries = mockk(relaxed = true)
    val chapters: ChaptersQueries = mockk(relaxed = true)
    val categories: CategoriesQueries = mockk(relaxed = true)
    val savedSearches: Saved_searchQueries = mockk(relaxed = true)
    val database: Database = mockk(relaxed = true)
    val getCategories: GetCategories = mockk()
    val sourceManager: SourceManager = mockk()
    val trackerManager: TrackerManager = mockk()
    val downloadCache: DownloadCache = mockk(relaxed = true)
    val storageManager: StorageManager = mockk()
    val getMangaByUrlAndSourceId: GetMangaByUrlAndSourceId = mockk()
    val getChaptersByMangaId: GetChaptersByMangaId = mockk()
    val getTracks: GetTracks = mockk()
    val insertTrack: InsertTrack = mockk(relaxed = true)
    val updateManga: UpdateManga = mockk(relaxed = true)
    val getFlatMetadataById: GetFlatMetadataById = mockk()
    val insertFlatMetadata: InsertFlatMetadata = mockk(relaxed = true)
    val setCustomMangaInfo: SetCustomMangaInfo = mockk(relaxed = true)
    val getExtensionStores: GetExtensionStores = mockk()
    val libraryPreferences = LibraryPreferences(store)
    val backupPreferences = BackupPreferences(store)

    init {
        every { database.mangasQueries } returns mangas
        every { database.chaptersQueries } returns chapters
        every { database.categoriesQueries } returns categories
        every { database.saved_searchQueries } returns savedSearches
        every { savedSearches.selectAll<BackupSavedSearch>(any()) } returns fakeQuery(emptyList())
        coEvery { database.transaction(any(), any()) } coAnswers {
            secondArg<suspend SuspendingTransactionWithoutReturn.() -> Unit>().invoke(mockk())
        }
        coEvery { getCategories.await() } returns emptyList()
        coEvery { getCategories.await(any()) } returns emptyList()
        every { mangas.getMangasWithFavoriteTimestamp() } returns fakeQuery(emptyList())
        every { mangas.getAllManga() } returns fakeQuery(emptyList())
        every { mangas.getAllMangaSourceAndUrl() } returns fakeQuery(emptyList())
        every { chapters.getChaptersByMangaId(any(), any()) } returns fakeQuery(emptyList())
        every { sourceManager.getAll() } returns emptyList()
        every { sourceManager.get(any()) } returns null
        coEvery { getMangaByUrlAndSourceId.await(any(), any()) } returns null
        coEvery { getChaptersByMangaId.await(any(), any()) } returns emptyList()
        coEvery { getTracks.await(any<Long>()) } returns emptyList()
        coEvery { getFlatMetadataById.await(any()) } returns null
        coEvery { getExtensionStores.get() } returns emptyList()
    }

    fun module(): Module = module {
        single { database }
        single { getCategories }
        single<ProtoBuf> { ProtoBuf }
        single { store }
        single { SecurityPreferences(store) }
        single { libraryPreferences }
        single { backupPreferences }
        single { sourceManager }
        single { trackerManager }
        single { downloadCache }
        single { storageManager }
        single<FetchInterval> { mockk { every { getWindow(any()) } returns (0L to 0L) } }
        single { getExtensionStores }
        single<GetFavorites> { mockk { coEvery { await() } returns emptyList() } }
        single<MangaRepository> { mockk { coEvery { getReadMangaNotInLibrary() } returns emptyList() } }
        single<GetMergedManga> { mockk { coEvery { await() } returns emptyList() } }
        single<GetHistory> { mockk { coEvery { await(any()) } returns emptyList() } }
        single { GetCustomMangaInfo(NoCustomInfo) }
        single { getFlatMetadataById }
        single { getMangaByUrlAndSourceId }
        single { getChaptersByMangaId }
        single { updateManga }
        single { getTracks }
        single { insertTrack }
        single { setCustomMangaInfo }
        single { insertFlatMetadata }
    }
}
