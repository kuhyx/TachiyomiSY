package eu.kanade.tachiyomi.data.download

import android.Manifest
import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkManager
import com.google.common.util.concurrent.Futures.immediateFuture
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.data.cache.ChapterCache
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.online.HttpSource
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.cancel
import kotlinx.serialization.json.Json
import nl.adaptivity.xmlutil.serialization.XML
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowStatFs
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.chapter.interactor.GetChapter
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.track.interactor.GetTracks
import java.io.File

internal fun imageResponse(body: String, code: Int = 200, type: String? = "image/png"): Response = Response.Builder()
    .request(Request.Builder().url("https://example.org/p.png").build())
    .protocol(Protocol.HTTP_1_1)
    .code(code)
    .message("ok")
    .body(body.toResponseBody(type?.toMediaType()))
    .build()

/**
 * A [Downloader] with every collaborator stubbed except the filesystem: [root] is a real downloads tree.
 * WorkManager is a relaxed mock, so the job start/stop calls land somewhere harmless.
 */
internal abstract class DownloaderTestBase {

    @get:Rule
    val tmp: TemporaryFolder = TemporaryFolder()

    protected val context: Context = ApplicationProvider.getApplicationContext()
    protected val store = MapPreferenceStore()
    protected val sourcePreferences = SourcePreferences(store)
    protected val sourceManager: SourceManager = mockk(relaxed = true)
    protected val chapterCache: ChapterCache = mockk(relaxed = true)
    protected val cache: DownloadCache = mockk(relaxed = true)
    protected val getCategories: GetCategories = mockk()
    protected val getTracks: GetTracks = mockk()
    protected val getManga: GetManga = mockk(relaxed = true)
    protected val getChapter: GetChapter = mockk(relaxed = true)
    protected val workManager: WorkManager = mockk(relaxed = true)
    protected val source: HttpSource = httpSource(name = "Source", id = 5L)
    protected val manga = Manga.create().copy(id = 1L, source = 5L, ogTitle = "Title")
    protected lateinit var root: File
    protected lateinit var provider: DownloadProviderHarness
    protected lateinit var downloader: Downloader

    @Before
    fun setUpDownloader() {
        Shadows.shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        root = tmp.newFolder("downloads")
        ShadowStatFs.registerStats(root, 1_000_000, 1_000_000, 1_000_000)
        provider = DownloadProviderHarness(root = root, context = context)
        provider.downloadPreferences.includeChapterUrlHash.set(false)
        mockkObject(WorkManager)
        every { WorkManager.getInstance(any<Context>()) } returns workManager
        every { workManager.getWorkInfosForUniqueWork(any()) } returns immediateFuture(emptyList())
        mockkStatic("eu.kanade.tachiyomi.data.download.DownloadCacheQueriesKt")
        coEvery { cache.addChapter(any(), any(), any()) } returns Unit
        every { sourceManager.get(5L) } returns source
        coEvery { getCategories.await(any()) } returns emptyList()
        coEvery { getTracks.await(any<Long>()) } returns emptyList()
        startKoin {
            modules(
                module {
                    single { sourceManager }
                    single<Json> { Json }
                    single { getManga }
                    single { getChapter }
                    single { SecurityPreferences(store) }
                },
            )
        }
        downloader = newDownloader()
    }

    @After
    fun tearDownDownloader() {
        downloader.scope.cancel()
        stopKoin()
        unmockkAll()
    }

    protected fun newDownloader(): Downloader = Downloader(
        context = context,
        provider = provider.provider,
        cache = cache,
        sourceManager = sourceManager,
        chapterCache = chapterCache,
        downloadPreferences = provider.downloadPreferences,
        xml = XML.v1 {},
        getCategories = getCategories,
        getTracks = getTracks,
        sourcePreferences = sourcePreferences,
    )

    protected fun chapter(id: Long, name: String = "Ch $id", order: Long = id): Chapter =
        Chapter.create().copy(id = id, mangaId = 1L, name = name, url = "/c/$id", sourceOrder = order)

    protected fun download(id: Long, owner: Manga = manga, from: HttpSource = source): Download =
        Download(from, owner, chapter(id))

    protected fun shownNotification(id: Int): android.app.Notification? =
        Shadows.shadowOf(context.getSystemService(android.app.NotificationManager::class.java)).getNotification(id)

    protected fun readyPages(count: Int): List<Page> = List(count) { Page(it, imageUrl = "https://i/$it.png") }
}
