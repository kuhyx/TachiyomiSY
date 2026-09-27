package eu.kanade.tachiyomi.data.library

import android.Manifest
import android.app.Application
import android.app.Notification
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Data
import androidx.work.ForegroundUpdater
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.common.util.concurrent.Futures.immediateFuture
import eu.kanade.domain.captureLogcat
import eu.kanade.domain.manga.interactor.UpdateManga
import eu.kanade.domain.releaseLogcat
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.sync.SyncPreferences
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.mdlist.MdList
import eu.kanade.tachiyomi.source.online.installSilentXLog
import eu.kanade.tachiyomi.ui.base.customInfoModule
import eu.kanade.tachiyomi.util.system.workManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import mihon.domain.chapter.interactor.FilterChaptersForDownload
import mihon.domain.source.interactor.UpdateMangaFromRemote
import mihon.domain.source.models.RemoteMangaUpdate
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.Shadows.shadowOf
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.interactor.FetchInterval
import tachiyomi.domain.manga.interactor.GetFavorites
import tachiyomi.domain.manga.interactor.GetLibraryManga
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.interactor.GetMergedMangaForDownloading
import tachiyomi.domain.manga.interactor.InsertFlatMetadata
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.interactor.InsertTrack
import java.util.UUID

/** The text a notification carries under [key]. */
internal fun Notification.extra(key: String): String? = extras.getCharSequence(key)?.toString()

/** A library entry for [manga] with the given chapter tallies. */
internal fun libraryEntry(
    manga: Manga,
    categories: List<Long> = emptyList(),
    total: Long = 0,
    read: Long = 0,
): LibraryManga = LibraryManga(
    manga = manga,
    categories = categories,
    totalChapters = total,
    readCount = read,
    bookmarkCount = 0,
    latestUpload = 0,
    chapterFetchedAt = 0,
    lastRead = 0,
)

internal fun libManga(id: Long, source: Long = 1L, title: String = "m$id"): Manga =
    Manga.create().copy(id = id, source = source, ogTitle = title, favorite = true)

internal fun libChapter(id: Long, mangaId: Long = 1L, number: Double = -1.0): Chapter =
    Chapter.create().copy(id = id, mangaId = mangaId, chapterNumber = number, sourceOrder = id)

/**
 * Every collaborator a [LibraryUpdateJob] / [MetadataUpdateJob] pulls from Injekt, as mocks and real
 * in-memory preferences, for one Robolectric test. [workManager] replaces `Context.workManager`.
 */
internal class LibraryJobHarness {
    val context: Application = ApplicationProvider.getApplicationContext()
    val store = InMemoryPreferenceStore()
    val libraryPreferences = LibraryPreferences(store)
    val securityPreferences = SecurityPreferences(store)
    val sourcePreferences = SourcePreferences(store)
    val syncPreferences = SyncPreferences(store)
    val sourceManager = mockk<SourceManager>()
    val downloadManager = mockk<DownloadManager>(relaxed = true)
    val getLibraryManga = mockk<GetLibraryManga>()
    val getManga = mockk<GetManga>()
    val fetchInterval = mockk<FetchInterval>()
    val filterChaptersForDownload = mockk<FilterChaptersForDownload>()
    val updateManga = mockk<UpdateManga>(relaxed = true)
    val updateMangaFromRemote = mockk<UpdateMangaFromRemote>()
    val getFavorites = mockk<GetFavorites>()
    val insertFlatMetadata = mockk<InsertFlatMetadata>(relaxed = true)
    val networkToLocalManga = mockk<NetworkToLocalManga>()
    val getMergedMangaForDownloading = mockk<GetMergedMangaForDownloading>()
    val getTracks = mockk<GetTracks>()
    val insertTrack = mockk<InsertTrack>(relaxed = true)
    val mdList = mockk<MdList>(relaxed = true)
    val trackerManager = mockk<TrackerManager>(relaxed = true)
    val workManager = mockk<WorkManager>(relaxed = true)
    val updater = mockk<ForegroundUpdater>()
    var logged = mutableListOf<String>()

    init {
        // Test classes build favourite Manga fixtures as fields, right after this harness and before
        // start(); a favourite Manga looks up its custom info through Injekt as it is constructed.
        stopKoin()
        startKoin { modules(customInfoModule()) }
    }

    fun start() {
        logged = captureLogcat()
        installSilentXLog()
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val mdListMock = mdList
        every { trackerManager.mdList } returns mdListMock
        every { mdList.isLoggedIn } returns false
        mockkStatic("eu.kanade.tachiyomi.util.system.WorkManagerExtensionsKt")
        every { any<Context>().workManager } returns workManager
        running(tag = LibraryUpdateJob.WORK_NAME_MANUAL, state = WorkInfo.State.SUCCEEDED)
        running(tag = LibraryUpdateJob.TAG, state = WorkInfo.State.SUCCEEDED)
        running(tag = SYNC_JOB_TAG, state = WorkInfo.State.SUCCEEDED)
        running(tag = METADATA_TAG, state = WorkInfo.State.SUCCEEDED)
        every { updater.setForegroundAsync(any(), any(), any()) } returns immediateFuture(null)
        every { sourceManager.get(any()) } returns null
        every { fetchInterval.getWindow(any()) } returns (0L to Long.MAX_VALUE)
        coEvery { getLibraryManga.await() } returns emptyList()
        stopKoin()
        startKoin { modules(module(), customInfoModule()) }
    }

    private fun module() = module {
        single { libraryPreferences }
        single { securityPreferences }
        single { sourcePreferences }
        single { syncPreferences }
        single { sourceManager }
        single { downloadManager }
        single { getLibraryManga }
        single { getManga }
        single { fetchInterval }
        single { filterChaptersForDownload }
        single { updateManga }
        single { updateMangaFromRemote }
        single { getFavorites }
        single { insertFlatMetadata }
        single { networkToLocalManga }
        single { getMergedMangaForDownloading }
        single { getTracks }
        single { insertTrack }
        single { trackerManager }
    }

    fun stop() {
        unmockkAll()
        stopKoin()
        releaseLogcat()
    }

    /** `WorkManager.getWorkInfosByTag(tag)` answers one job in [state]. */
    fun running(tag: String, state: WorkInfo.State) {
        val info = WorkInfo(UUID.randomUUID(), state, setOf(tag))
        every { workManager.getWorkInfosByTag(tag) } returns immediateFuture(listOf(info))
    }

    fun params(input: Data = Data.EMPTY, vararg tags: String): WorkerParameters {
        val params = mockk<WorkerParameters>(relaxed = true)
        every { params.tags } returns tags.toSet()
        every { params.id } returns UUID.randomUUID()
        every { params.inputData } returns input
        every { params.foregroundUpdater } returns updater
        return params
    }

    fun job(input: Data = Data.EMPTY, vararg tags: String): LibraryUpdateJob =
        LibraryUpdateJob(context, params(input, *tags))

    /** Any remote update of [manga] yields [newChapters] (with [favorite] on the refreshed manga). */
    fun stubRemote(manga: Manga, newChapters: List<Chapter> = emptyList(), favorite: Boolean = true) {
        val result = Result.success(RemoteMangaUpdate(manga.copy(favorite = favorite), newChapters))
        coEvery {
            updateMangaFromRemote(
                source = any(),
                manga = manga,
                fetchDetails = any(),
                fetchChapters = any(),
                manualFetch = any(),
                fetchWindow = any(),
                throttleFunc = any(),
            )
        } returns result
    }

    fun stubRemoteFailure(manga: Manga, error: Throwable) {
        coEvery {
            updateMangaFromRemote(
                source = any(),
                manga = manga,
                fetchDetails = any(),
                fetchChapters = any(),
                manualFetch = any(),
                fetchWindow = any(),
                throttleFunc = any(),
            )
        } returns Result.failure(error)
    }

    companion object {
        const val SYNC_JOB_TAG = "SyncDataJob"
        const val METADATA_TAG = "MetadataUpdate"
    }
}
