package eu.kanade.tachiyomi.data.sync

import android.Manifest
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import app.cash.sqldelight.SuspendingTransactionWithoutReturn
import eu.kanade.domain.FlowPreferenceStore
import eu.kanade.domain.captureLogcat
import eu.kanade.domain.manga.interactor.UpdateManga
import eu.kanade.domain.releaseLogcat
import eu.kanade.domain.sync.SyncPreferences
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.data.backup.fakeQuery
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.BackupSavedSearch
import eu.kanade.tachiyomi.data.backup.restore.BackupRestoreJob
import eu.kanade.tachiyomi.data.sync.service.SyncData
import eu.kanade.tachiyomi.data.sync.service.SyncYomiSyncService
import eu.kanade.tachiyomi.source.model.UpdateStrategy
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkAll
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.protobuf.ProtoBuf
import mihon.domain.extension.interactor.GetExtensionStores
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.Shadows.shadowOf
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.data.CategoriesQueries
import tachiyomi.data.ChaptersQueries
import tachiyomi.data.Database
import tachiyomi.data.GetMangasWithFavoriteTimestamp
import tachiyomi.data.Mangas
import tachiyomi.data.MangasQueries
import tachiyomi.data.Saved_searchQueries
import tachiyomi.domain.backup.service.BackupPreferences
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.history.interactor.GetHistory
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
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.interactor.InsertTrack

/**
 * Everything [SyncManager] pulls from Injekt, for one Robolectric test: a mocked [Database] whose
 * query sets the tests stub, the real preferences, and every backup creator and restorer
 * collaborator as an empty-answering mock. [remote] is what the SyncYomi service's `doSync` returns.
 */
internal class SyncManagerHarness {
    val context: Application = ApplicationProvider.getApplicationContext()
    val store = FlowPreferenceStore()
    val preferences = SyncPreferences(store)
    val mangas: MangasQueries = mockk(relaxed = true)
    val chapters: ChaptersQueries = mockk(relaxed = true)
    val categories: CategoriesQueries = mockk(relaxed = true)
    val database: Database = mockk(relaxed = true)
    val getCategories: GetCategories = mockk()
    var remote: (SyncData) -> Backup? = { it.backup }
    var logged = mutableListOf<String>()

    fun start() {
        logged = captureLogcat()
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        every { database.mangasQueries } returns mangas
        every { database.chaptersQueries } returns chapters
        every { database.categoriesQueries } returns categories
        every { database.saved_searchQueries } returns mockk<Saved_searchQueries> {
            every { selectAll<BackupSavedSearch>(any()) } returns fakeQuery(emptyList())
        }
        coEvery { database.transaction(any(), any()) } coAnswers {
            secondArg<suspend SuspendingTransactionWithoutReturn.() -> Unit>().invoke(mockk())
        }
        coEvery { getCategories.await() } returns emptyList()
        coEvery { getCategories.await(any()) } returns emptyList()
        every { mangas.getMangasWithFavoriteTimestamp() } returns fakeQuery(emptyList())
        every { mangas.getAllManga() } returns fakeQuery(emptyList())
        every { chapters.getChaptersByMangaId(any(), any()) } returns fakeQuery(emptyList())
        startKoin { modules(module()) }
        mockkObject(BackupRestoreJob)
        every { BackupRestoreJob.start(any(), any(), any(), any()) } just runs
        mockkConstructor(SyncYomiSyncService::class)
        coEvery { anyConstructed<SyncYomiSyncService>().doSync(any()) } answers { remote(firstArg()) }
        preferences.syncService.set(SyncManager.SyncService.SYNCYOMI.value)
    }

    /** Asserts `persistManga` wrote [favorite] onto the row [mangaId] ([times] times, or at all) as syncing. */
    fun verifyFavoriteWritten(mangaId: Long, favorite: Boolean, times: Int = -1) = coVerify(exactly = times) {
        mangas.update(
            source = any(),
            url = any(),
            artist = any(),
            author = any(),
            description = any(),
            genre = any(),
            title = any(),
            status = any(),
            thumbnailUrl = any(),
            favorite = favorite,
            lastUpdate = any(),
            nextUpdate = any(),
            initialized = any(),
            viewer = any(),
            chapterFlags = any(),
            coverLastModified = any(),
            dateAdded = any(),
            updateStrategy = any(),
            calculateInterval = any(),
            version = any(),
            isSyncing = 1L,
            notes = any(),
            memo = any(),
            mangaId = mangaId,
        )
    }

    fun stop() {
        unmockkAll()
        stopKoin()
        releaseLogcat()
    }

    private fun module() = module {
        single { database }
        single { preferences }
        single { getCategories }
        single<ProtoBuf> { ProtoBuf }
        single<PreferenceStore> { store }
        single { SecurityPreferences(InMemoryPreferenceStore()) }
        single<SourceManager> { mockk { every { getAll() } returns emptyList() } }
        single<FetchInterval> { mockk { every { getWindow(any()) } returns (0L to 0L) } }
        single<GetExtensionStores> { mockk { coEvery { get() } returns emptyList() } }
        single<GetFavorites> { mockk() }
        single<BackupPreferences> { mockk() }
        single<MangaRepository> { mockk() }
        single<GetMergedManga> { mockk() }
        single<GetHistory> { mockk() }
        single { GetCustomMangaInfo(NoCustomInfo) }
        single<GetFlatMetadataById> { mockk() }
        single<GetMangaByUrlAndSourceId> { mockk() }
        single<GetChaptersByMangaId> { mockk() }
        single<UpdateManga> { mockk() }
        single<GetTracks> { mockk() }
        single<InsertTrack> { mockk() }
        single<SetCustomMangaInfo> { mockk() }
        single<InsertFlatMetadata> { mockk() }
    }
}

/** No entry has custom info; a real [GetCustomMangaInfo] over it serves `Manga`'s companion lookup. */
internal object NoCustomInfo : CustomMangaRepository {
    override fun get(mangaId: Long): CustomMangaInfo? = null

    override fun set(mangaInfo: CustomMangaInfo) = Unit
}

internal fun mangasRow(id: Long, url: String, favorite: Boolean = true, version: Long = 0): Mangas = Mangas(
    _id = id,
    source = 1L,
    url = url,
    artist = null,
    author = null,
    description = null,
    genre = null,
    title = url,
    status = 0L,
    thumbnail_url = null,
    favorite = favorite,
    last_update = null,
    next_update = null,
    initialized = false,
    viewer = 0L,
    chapter_flags = 0L,
    cover_last_modified = 0L,
    date_added = 0L,
    filtered_scanlators = null,
    update_strategy = UpdateStrategy.ALWAYS_UPDATE,
    calculate_interval = 0L,
    last_modified_at = 0L,
    favorite_modified_at = null,
    version = version,
    is_syncing = 0L,
    notes = "",
    memo = JsonObject(emptyMap()),
)

internal fun favoriteRow(id: Long, url: String): GetMangasWithFavoriteTimestamp = mangasRow(id, url).let {
    GetMangasWithFavoriteTimestamp(
        _id = it._id,
        source = it.source,
        url = it.url,
        artist = null,
        author = null,
        description = null,
        genre = null,
        title = it.title,
        status = 0L,
        thumbnail_url = null,
        favorite = true,
        last_update = null,
        next_update = null,
        initialized = false,
        viewer = 0L,
        chapter_flags = 0L,
        cover_last_modified = 0L,
        date_added = 0L,
        filtered_scanlators = null,
        update_strategy = it.update_strategy,
        calculate_interval = 0L,
        last_modified_at = 0L,
        favorite_modified_at = 1L,
        version = 0L,
        is_syncing = 0L,
        notes = "",
        memo = it.memo,
    )
}
