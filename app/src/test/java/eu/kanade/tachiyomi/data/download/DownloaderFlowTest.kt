package eu.kanade.tachiyomi.data.download

import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.source.model.Page
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
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
import tachiyomi.core.metadata.comicinfo.COMIC_INFO_FILE
import java.io.File
import java.io.IOException

/** A whole chapter through [downloadChapter], saved as a plain directory. */
@RunWith(RobolectricTestRunner::class)
internal class DownloaderFlowTest : DownloaderTestBase() {

    private val chapterDir: File
        get() = File(root, "Source/Title/Ch 1")

    @Before
    fun setUpSource() {
        provider.downloadPreferences.saveChaptersAsCBZ.set(false)
        mockkObject(ImageUtil)
        every { ImageUtil.splitTallImage(any(), any(), any()) } returns true
        every { ImageUtil.getExtensionFromMimeType(any(), any()) } returns "png"
        coEvery { source.getPageList(any()) } returns readyPages(2)
        coEvery { source.getImage(any(), any()) } answers { imageResponse("png") }
        every { source.getChapterUrl(any()) } returns "https://source/c/1"
    }

    @Test
    fun chapterIsDownloaded() = runTest {
        val download = download(1L)
        downloader.downloadChapter(download)
        download.status shouldBe Download.State.DOWNLOADED
        chapterDir.list()!!.sorted() shouldBe listOf(".nomedia", "001.png", "002.png", COMIC_INFO_FILE)
        coVerify { cache.addChapter("Ch 1", any(), manga) }
    }

    @Test
    fun knownPagesAreNotFetchedAgain() = runTest {
        val download = download(1L).apply { pages = readyPages(1) }
        downloader.downloadChapter(download)
        download.status shouldBe Download.State.DOWNLOADED
        coVerify(exactly = 0) { source.getPageList(any()) }
    }

    @Test
    fun missingPageFailsTheChapter() = runTest {
        coEvery { source.getImage(match { it.index == 1 }, any()) } throws IOException("gone")
        val download = download(1L)
        downloader.downloadChapter(download)
        download.status shouldBe Download.State.ERROR
        File(root, "Source/Title/Ch 1_tmp/001.png").exists() shouldBe true
    }

    @Test
    fun pageListFailureIsReported() = runTest {
        coEvery { source.getPageList(any()) } throws IOException("offline")
        val download = download(1L)
        downloader.downloadChapter(download)
        download.status shouldBe Download.State.ERROR
        (shownNotification(Notifications.ID_DOWNLOAD_CHAPTER_ERROR) != null) shouldBe true
    }

    @Test
    fun noMangaDirStopsEarly() = runTest {
        every { provider.storageManager.getDownloadsDirectory() } returns null
        val download = download(1L)
        downloader.downloadChapter(download)
        coVerify(exactly = 0) { source.getPageList(any()) }
    }

    @Test
    fun cancellationIsRethrown() = runTest {
        coEvery { source.getPageList(any()) } coAnswers { awaitCancellation() }
        val download = download(1L)
        val job = launch { downloader.downloadChapter(download) }
        testScheduler.runCurrent()
        job.cancel()
        job.join()
        download.status shouldBe Download.State.NOT_DOWNLOADED
    }

    @Test
    fun missingUrlsAreResolved() = runTest {
        sourcePreferences.dataSaverDownloader.set(true)
        coEvery { source.getPageList(any()) } returns listOf(Page(0), Page(1, imageUrl = ""))
        coEvery { source.getImageUrl(match { it.index == 0 }) } returns "https://i/0.png"
        coEvery { source.getImageUrl(match { it.index == 1 }) } throws IOException("no url")
        val download = download(1L)
        downloader.downloadChapter(download)
        download.pages!!.map { it.imageUrl } shouldBe listOf("https://i/0.png", "")
    }
}
