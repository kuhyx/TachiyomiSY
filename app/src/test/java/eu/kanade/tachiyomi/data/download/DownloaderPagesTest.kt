package eu.kanade.tachiyomi.data.download

import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.util.storage.DiskUtil
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.util.system.ImageUtil
import tachiyomi.core.metadata.comicinfo.COMIC_INFO_FILE
import java.io.File
import java.io.InputStream

/** Naming, splitting and the final completeness check of a chapter's pages. */
@RunWith(RobolectricTestRunner::class)
internal class DownloaderPagesTest : DownloaderTestBase() {

    private lateinit var work: File
    private lateinit var workDir: UniFile

    @Before
    fun setUpDir() {
        work = File(root, "work").apply { mkdirs() }
        workDir = UniFile.fromFile(work)!!
        mockkObject(ImageUtil)
        every { ImageUtil.splitTallImage(any(), any(), any()) } returns true
        every { ImageUtil.getExtensionFromMimeType(any(), any()) } answers {
            if (firstArg<String?>() == "image/png") "png" else "webp"
        }
        every { ImageUtil.findImageType(any<InputStream>()) } returns ImageUtil.ImageType.WEBP
    }

    private fun file(name: String): UniFile = UniFile.fromFile(File(work, name).apply { writeText("x") })!!

    @Test
    fun extensionComesFromTheMimeType() {
        downloader.getImageExtension(imageResponse(body = "x", type = "image/png"), file("a")) shouldBe "png"
        downloader.getImageExtension(imageResponse(body = "x", type = "text/html"), file("b")) shouldBe "webp"
        downloader.getImageExtension(imageResponse(body = "x", type = null), file("c")) shouldBe "webp"
    }

    @Test
    fun splittingIsOptIn() {
        provider.downloadPreferences.splitTallImages.set(false)
        downloader.splitTallImageIfNeeded(Page(0), workDir)
        verify(exactly = 0) { ImageUtil.splitTallImage(any(), any(), any()) }
    }

    @Test
    fun tallPagesAreSplit() {
        provider.downloadPreferences.splitTallImages.set(true)
        file("001.png")
        downloader.splitTallImageIfNeeded(Page(0), workDir)
        verify { ImageUtil.splitTallImage(workDir, any(), "001") }
    }

    @Test
    fun splitPagesAreLeftAlone() {
        provider.downloadPreferences.splitTallImages.set(true)
        file("001__001.jpg")
        downloader.splitTallImageIfNeeded(Page(0), workDir)
        verify(exactly = 0) { ImageUtil.splitTallImage(any(), any(), any()) }
    }

    @Test
    fun splitFailuresAreSwallowed() {
        provider.downloadPreferences.splitTallImages.set(true)
        downloader.splitTallImageIfNeeded(Page(4), workDir)
        file("005.png")
        every { ImageUtil.splitTallImage(any(), any(), any()) } throws IllegalStateException("oom")
        downloader.splitTallImageIfNeeded(Page(4), workDir)
        verify { ImageUtil.splitTallImage(any(), any(), "005") }
    }

    @Test
    fun successNeedsEveryPage() {
        val download = download(1L)
        downloader.isDownloadSuccessful(download, workDir) shouldBe false
        download.pages = readyPages(2).onEach { it.status = Page.State.Ready }
        downloader.isDownloadSuccessful(download, workDir) shouldBe false
        listOf("001.png", "002__001.jpg", "002__002.jpg", "003.png.tmp", COMIC_INFO_FILE, DiskUtil.NOMEDIA_FILE)
            .forEach(::file)
        downloader.isDownloadSuccessful(download, workDir) shouldBe true
    }

    @Test
    fun successNeedsEveryImageReady() {
        val download = download(1L).apply { pages = readyPages(2) }
        file("001.png")
        file("002.png")
        downloader.isDownloadSuccessful(download, workDir) shouldBe false
    }

    @Test
    fun pageImagesAreRecognised() {
        downloader.isDownloadedPageImage("001.png", "001") shouldBe true
        downloader.isDownloadedPageImage("001__001.webp", "001") shouldBe true
        downloader.isDownloadedPageImage("001.png.tmp", "001") shouldBe false
        downloader.isDownloadedPageImage("0011.png", "001") shouldBe false
        downloader.isDownloadedPageImage("001__002.png", "001") shouldBe false
        inProgressFileName("001") shouldBe "001.tmp"
    }
}
