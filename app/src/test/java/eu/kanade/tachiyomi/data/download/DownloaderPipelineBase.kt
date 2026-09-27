package eu.kanade.tachiyomi.data.download

import android.app.Notification
import com.hippo.unifile.UniFile
import eu.kanade.domain.captureLogcat
import eu.kanade.domain.installFakeAndroidKeyStore
import eu.kanade.domain.releaseLogcat
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.util.storage.DiskUtil
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockkObject
import org.junit.After
import org.junit.Before
import tachiyomi.core.common.util.system.ImageUtil
import java.io.File
import java.io.InputStream
import java.util.concurrent.atomic.AtomicInteger

/**
 * A [DownloaderTestBase] whose source serves a two-page chapter and whose reader cache already holds
 * every image, so a download runs end to end without the network. [space] is the free disk space.
 */
internal abstract class DownloaderPipelineBase : DownloaderTestBase() {

    // Pages download in parallel on IO threads, so each gets its own cache file from an atomic counter.
    private val cached = AtomicInteger()
    protected var space = 1L shl 40
    protected var logged = mutableListOf<String>()

    @Before
    fun stubPipeline() {
        logged = captureLogcat()
        // ComicInfo asks CbzCrypto for padding, and CbzCrypto opens the AndroidKeyStore when first touched.
        installFakeAndroidKeyStore()
        // CBZ output goes through libarchive's JNI, which a JVM test cannot load; folders it is.
        provider.downloadPreferences.saveChaptersAsCBZ.set(false)
        // Splitting decodes through the native image decoder; DownloaderPagesTest covers it with a mock.
        provider.downloadPreferences.splitTallImages.set(false)
        mockkObject(ImageUtil, DiskUtil)
        every { ImageUtil.findImageType(any<InputStream>()) } returns ImageUtil.ImageType.PNG
        every { DiskUtil.getAvailableStorageSpace(any<UniFile>()) } answers { space }
        every { chapterCache.isImageInCache(any()) } returns true
        every { chapterCache.getImageFile(any()) } answers {
            File(root, "cache${cached.getAndIncrement()}").apply { writeText("png") }
        }
        coEvery { source.getPageList(any()) } returns
            listOf(Page(7, imageUrl = "https://i/1"), Page(8, imageUrl = "https://i/2"))
        coEvery { source.getImageUrl(any()) } returns "https://i/2"
        every { source.getChapterUrl(any()) } returns "https://c/1"
    }

    /** Asserts the download finished, naming the downloader's last error when it did not. */
    protected fun Download.shouldBeDownloaded() {
        val error = shownNotification(Notifications.ID_DOWNLOAD_CHAPTER_ERROR)
            ?.extras
            ?.let { listOf(Notification.EXTRA_TEXT, Notification.EXTRA_BIG_TEXT).map(it::getCharSequence) }
        val pageErrors = pages.orEmpty().mapNotNull { (it.status as? Page.State.Error)?.error?.stackTraceToString() }
        val clue = "last download error: $error; page errors: ${pageErrors.map { it.take(CLUE_CHARS) }}; " +
            "log: ${logged.joinToString().take(CLUE_CHARS)}"
        withClue(clue) {
            status shouldBe Download.State.DOWNLOADED
        }
    }

    protected fun newDownloaderWith(provider: DownloadProvider): Downloader = Downloader(
        context = context,
        provider = provider,
        cache = cache,
        sourceManager = sourceManager,
        chapterCache = chapterCache,
        downloadPreferences = this.provider.downloadPreferences,
        xml = downloader.xml,
        getCategories = getCategories,
        getTracks = getTracks,
        sourcePreferences = sourcePreferences,
    )

    @After
    fun releasePipelineLog() = releaseLogcat()

    private companion object {
        const val CLUE_CHARS = 2_000
    }
}
