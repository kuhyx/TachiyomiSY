package eu.kanade.tachiyomi.data.download

import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.network.HttpException
import eu.kanade.tachiyomi.source.model.Page
import exh.source.EH_SOURCE_ID
import exh.util.DataSaver
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockkObject
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.util.system.ImageUtil
import java.io.File
import java.io.InputStream

/** Fetching one page: from the chapter directory, the reader's cache, or the network (with retries). */
@RunWith(RobolectricTestRunner::class)
internal class DownloaderImagesTest : DownloaderTestBase() {

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
        every { ImageUtil.findImageType(any<InputStream>()) } returns ImageUtil.ImageType.PNG
    }

    private fun page(number: Int = 1, url: String? = "https://i/1.png") = Page(number - 1, imageUrl = url)

    private suspend fun fetch(page: Page, pages: Int? = null) {
        val download = download(1L).apply { this.pages = pages?.let(::readyPages) }
        downloader.getOrDownloadImage(page, download, workDir, DataSaver.NoOp)
    }

    @Test
    fun pageWithoutUrlIsSkipped() = runTest {
        val page = page(url = null)
        fetch(page)
        page.status shouldBe Page.State.Queue
    }

    @Test
    fun existingImageIsReused() = runTest {
        File(work, "001.png").writeText("png")
        File(work, "002.png.tmp").writeText("partial")
        val page = page()
        fetch(page, pages = 12)
        page.status shouldBe Page.State.Ready
        page.uri shouldBe UniFile.fromFile(File(work, "001.png"))!!.uri
    }

    @Test
    fun cachedImageIsCopied() = runTest {
        val cached = File(root, "cached").apply { writeText("png") }
        every { chapterCache.isImageInCache("https://i/1.png") } returns true
        every { chapterCache.getImageFile("https://i/1.png") } returns cached
        File(work, "001.tmp").writeText("stale")
        fetch(page())
        File(work, "001.png").readText() shouldBe "png"
        cached.exists() shouldBe false
    }

    @Test
    fun unknownCachedTypeKeepsTmp() {
        val cached = File(root, "cached").apply { writeText("???") }
        every { ImageUtil.findImageType(any<InputStream>()) } returns null
        downloader.copyImageFromCache(cached, workDir, "001").name shouldBe "001.tmp"
        cached.exists() shouldBe true
    }

    @Test
    fun networkImageIsSaved() = runTest {
        coEvery { source.getImage(any(), any()) } returns imageResponse("png-bytes")
        val page = page()
        fetch(page)
        File(work, "001.png").readText() shouldBe "png-bytes"
        page.progress shouldBe 100
    }

    @Test
    fun partialContentIsAppended() = runTest {
        File(work, "001.tmp").writeText("head-")
        coEvery { source.getImage(any(), any()) } returns imageResponse(body = "tail", code = 206)
        fetch(page())
        File(work, "001.png").readText() shouldBe "head-tail"
    }

    @Test
    fun failuresAreRetriedThenReported() = runTest {
        coEvery { source.getImage(any(), any()) } throws HttpException(500)
        val page = page()
        fetch(page)
        (page.status as Page.State.Error).error.message shouldBe "HTTP error 500"
    }

    @Test
    fun badRangeDropsThePartial() = runTest {
        File(work, "001.tmp").writeText("too long")
        coEvery { source.getImage(any(), any()) } throws HttpException(HTTP_RANGE_NOT_SATISFIABLE)
        shouldThrow<HttpException> { downloader.downloadImage(page(), source, workDir, "001", DataSaver.NoOp) }
        File(work, "001.tmp").exists() shouldBe false
    }

    @Test
    fun ehRetriesRefreshTheUrl() = runTest {
        val eh = httpSource(name = "E-Hentai", id = EH_SOURCE_ID)
        coEvery { eh.getImage(any(), any()) } throws HttpException(500) andThen imageResponse("png")
        coEvery { eh.getImageUrl(any()) } returns "https://i/fresh.png"
        val page = page()
        downloader.downloadImage(page, eh, workDir, "001", DataSaver.NoOp)
        page.imageUrl shouldBe "https://i/fresh.png"
    }

    @Test
    fun cancellationIsNotAnError() = runTest {
        coEvery { source.getImage(any(), any()) } coAnswers { awaitCancellation() }
        val page = page()
        val job = launch { fetch(page) }
        testScheduler.runCurrent()
        job.cancel()
        job.join()
        (page.status is Page.State.Error) shouldBe false
    }
}
