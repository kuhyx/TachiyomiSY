package eu.kanade.tachiyomi.data.download

import android.Manifest
import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.common.util.concurrent.Futures.immediateFuture
import eu.kanade.domain.captureLogcat
import eu.kanade.domain.releaseLogcat
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.data.cache.ChapterCache
import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.installSilentXLog
import eu.kanade.tachiyomi.ui.base.customInfoModule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import kotlinx.coroutines.cancel
import kotlinx.serialization.json.Json
import nl.adaptivity.xmlutil.serialization.XML
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.Shadows.shadowOf
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.chapter.interactor.GetChapter
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.track.interactor.GetTracks
import java.io.File
import java.util.UUID

/**
 * A real [DownloadManager] (and so a real [Downloader] and pending deleter) over a temp downloads
 * tree in [root] and a mocked [DownloadCache]; every Injekt default the downloader pulls is served.
 */
internal abstract class DownloadManagerTestBase {

    @get:Rule
    val tmp: TemporaryFolder = TemporaryFolder()

    protected val context: Application = ApplicationProvider.getApplicationContext()
    protected val store = MapPreferenceStore()
    protected val source: HttpSource = httpSource(name = "Source", id = 5L)
    protected val sourceManager: SourceManager = mockk(relaxed = true)
    protected val cache: DownloadCache = mockk(relaxed = true)
    protected val getCategories: GetCategories = mockk()
    protected val getChapter: GetChapter = mockk()
    protected val getManga: GetManga = mockk()
    protected val workManager: WorkManager = mockk(relaxed = true)

    // Built after Koin starts: a favourite Manga looks its custom info up through Injekt.
    protected val manga: Manga by lazy { Manga.create().copy(id = 1L, source = 5L, ogTitle = "Title", favorite = true) }
    protected lateinit var root: File
    protected lateinit var provider: DownloadProviderHarness
    protected lateinit var manager: DownloadManager
    protected var logged = mutableListOf<String>()

    @Before
    fun setUpManager() {
        logged = captureLogcat()
        installSilentXLog()
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        root = tmp.newFolder("downloads")
        provider = DownloadProviderHarness(root = root, context = context)
        provider.downloadPreferences.includeChapterUrlHash.set(false)
        mockkObject(WorkManager)
        every { WorkManager.getInstance(any<Context>()) } returns workManager
        running(false)
        stubCache()
        every { sourceManager.get(5L) } returns source
        every { sourceManager.getOrStub(5L) } returns source
        coEvery { getCategories.await(any()) } returns emptyList()
        startKoin { modules(module(), customInfoModule()) }
        manager = DownloadManager(
            context = context,
            provider = provider.provider,
            cache = cache,
            getCategories = getCategories,
            sourceManager = sourceManager,
            downloadPreferences = provider.downloadPreferences,
        )
    }

    // Every cache operation is an extension function, so the relaxed mock alone would run the real ones.
    private fun stubCache() {
        mockkStatic(
            "eu.kanade.tachiyomi.data.download.DownloadCacheQueriesKt",
            "eu.kanade.tachiyomi.data.download.DownloadCacheRemovalsKt",
        )
        every { cache.isChapterDownloaded(any(), any(), any(), any(), any(), any()) } returns false
        every { cache.getTotalDownloadCount() } returns 0
        every { cache.getDownloadCount(any()) } returns 0
        coEvery { cache.addChapter(any(), any(), any()) } just runs
        coEvery { cache.removeChapter(any(), any()) } just runs
        coEvery { cache.removeChapters(any(), any()) } just runs
        coEvery { cache.removeFolders(any(), any()) } just runs
        coEvery { cache.removeManga(any()) } just runs
        coEvery { cache.renameManga(any(), any(), any()) } just runs
        coEvery { cache.removeSource(any()) } just runs
    }

    private fun module() = module {
        single { sourceManager }
        single { provider.provider }
        single { cache }
        single<ChapterCache> { mockk(relaxed = true) }
        single { provider.downloadPreferences }
        single { XML.v1 {} }
        single { getCategories }
        single<GetTracks> { mockk(relaxed = true) }
        single { SourcePreferences(store) }
        single { SecurityPreferences(store) }
        single<Json> { Json }
        single { getManga }
        single { getChapter }
    }

    @After
    fun tearDownManager() {
        manager.downloader.scope.cancel()
        stopKoin()
        unmockkAll()
        releaseLogcat()
    }

    /** Whether WorkManager reports the download job as running. */
    protected fun running(isRunning: Boolean) {
        val state = if (isRunning) WorkInfo.State.RUNNING else WorkInfo.State.SUCCEEDED
        val info = WorkInfo(UUID.randomUUID(), state, setOf("Downloader"))
        every { workManager.getWorkInfosForUniqueWork("Downloader") } returns immediateFuture(listOf(info))
    }

    protected fun chapter(id: Long, read: Boolean = false, bookmark: Boolean = false): Chapter = Chapter.create()
        .copy(id = id, mangaId = manga.id, name = "Ch $id", url = "/c/$id", read = read, bookmark = bookmark)

    /** Creates the on-disk directory of [chapter] with one page in it. */
    protected fun downloaded(chapter: Chapter, title: String = "Title"): File =
        File(root, "Source/$title/${chapter.name}").apply {
            mkdirs()
            File(this, "001.png").writeText("png")
        }
}
