package eu.kanade.tachiyomi.data.library

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Data
import androidx.work.WorkerParameters
import eu.kanade.domain.captureLogcat
import eu.kanade.domain.manga.interactor.UpdateManga
import eu.kanade.domain.releaseLogcat
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.startDownloads
import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.mdlist.MdList
import eu.kanade.tachiyomi.data.updater.allowNotifications
import eu.kanade.tachiyomi.source.online.installSilentXLog
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import mihon.domain.chapter.interactor.FilterChaptersForDownload
import mihon.domain.source.interactor.UpdateMangaFromRemote
import org.junit.After
import org.junit.Before
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.interactor.FetchInterval
import tachiyomi.domain.manga.interactor.GetCustomMangaInfo
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

/** Every collaborator of [LibraryUpdateJob] as a mock in Koin, over real preferences and notifications. */
internal abstract class LibraryUpdateTestBase {

    protected val context: Context = ApplicationProvider.getApplicationContext()
    protected val store = MapPreferenceStore()
    protected val libraryPreferences = LibraryPreferences(store)
    protected val securityPreferences = SecurityPreferences(store)
    protected val sourceManager = mockk<SourceManager>(relaxed = true)
    protected val downloadManager = mockk<DownloadManager>(relaxed = true)
    protected val getLibraryManga = mockk<GetLibraryManga>()
    protected val getManga = mockk<GetManga>()
    protected val fetchInterval = mockk<FetchInterval>()
    protected val filterChapters = mockk<FilterChaptersForDownload>()
    protected val updateManga = mockk<UpdateManga>(relaxed = true)
    protected val updateFromRemote = mockk<UpdateMangaFromRemote>()
    protected val getFavorites = mockk<GetFavorites>()
    protected val insertFlatMetadata = mockk<InsertFlatMetadata>(relaxed = true)
    protected val networkToLocalManga = mockk<NetworkToLocalManga>()
    protected val getMerged = mockk<GetMergedMangaForDownloading>()
    protected val getTracks = mockk<GetTracks>()
    protected val insertTrack = mockk<InsertTrack>(relaxed = true)
    protected val mdList = mockk<MdList>(relaxed = true)
    protected val trackerManager = mockk<TrackerManager>(relaxed = true)
    protected lateinit var logged: MutableList<String>

    @Before
    fun setUpLibraryKoin() {
        context.allowNotifications()
        installSilentXLog()
        logged = captureLogcat()
        every { trackerManager.mdList } returns mdList
        // Starting downloads enqueues the downloader worker; WorkManager is not initialised here.
        mockkStatic("eu.kanade.tachiyomi.data.download.DownloadManagerQueueKt")
        every { downloadManager.startDownloads() } returns Unit
        every { fetchInterval.getWindow(any()) } returns (0L to Long.MAX_VALUE)
        val customInfo = mockk<GetCustomMangaInfo>().also { every { it.get(any()) } returns null }
        startKoin {
            modules(
                module {
                    single { sourceManager }
                    single { libraryPreferences }
                    single { securityPreferences }
                    single { downloadManager }
                    single { getLibraryManga }
                    single { getManga }
                    single { fetchInterval }
                    single { filterChapters }
                    single { updateManga }
                    single { updateFromRemote }
                },
                module {
                    single { getFavorites }
                    single { insertFlatMetadata }
                    single { networkToLocalManga }
                    single { getMerged }
                    single { getTracks }
                    single { insertTrack }
                    single { trackerManager }
                    single { customInfo }
                },
            )
        }
    }

    @After
    fun tearDownLibraryKoin() {
        releaseLogcat()
        stopKoin()
        unmockkAll()
    }

    protected fun params(input: Data = Data.EMPTY, tags: Set<String> = emptySet()): WorkerParameters =
        mockk<WorkerParameters>(relaxed = true).also {
            every { it.inputData } returns input
            every { it.tags } returns tags
        }

    protected fun job(input: Data = Data.EMPTY, tags: Set<String> = emptySet()): LibraryUpdateJob =
        LibraryUpdateJob(context, params(input, tags))

    protected fun shown(id: Int): Notification? =
        context.getSystemService(NotificationManager::class.java).activeNotifications
            .firstOrNull { it.id == id }
            ?.notification
}

internal fun manga(id: Long, source: Long = 1L, title: String = "Manga $id"): Manga =
    Manga.create().copy(id = id, source = source, ogTitle = title, favorite = true)

internal fun libraryManga(manga: Manga, categories: List<Long> = listOf(0L), total: Long = 0L, read: Long = 0L) =
    LibraryManga(
        manga = manga,
        categories = categories,
        totalChapters = total,
        readCount = read,
        bookmarkCount = 0L,
        latestUpload = 0L,
        chapterFetchedAt = 0L,
        lastRead = 0L,
    )

internal fun chapter(id: Long, number: Double = id.toDouble(), mangaId: Long = 1L): Chapter =
    Chapter.create().copy(id = id, mangaId = mangaId, chapterNumber = number, name = "Ch $id")
