package eu.kanade.tachiyomi.data.download

import eu.kanade.tachiyomi.data.download.model.Download
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockkObject
import kotlinx.coroutines.awaitCancellation
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.util.system.ImageUtil
import java.io.IOException

/** The downloader job: which downloads run at once, and how the set changes as they end. */
@RunWith(RobolectricTestRunner::class)
internal class DownloaderRunTest : DownloaderTestBase() {

    private val other = httpSource(name = "Other", id = 6L)

    @Before
    fun setUpSources() {
        provider.downloadPreferences.saveChaptersAsCBZ.set(false)
        mockkObject(ImageUtil)
        every { ImageUtil.splitTallImage(any(), any(), any()) } returns true
        every { ImageUtil.getExtensionFromMimeType(any(), any()) } returns "png"
        every { sourceManager.get(6L) } returns other
        for (s in listOf(source, other)) {
            coEvery { s.getPageList(any()) } returns readyPages(1)
            coEvery { s.getImage(any(), any()) } answers { imageResponse("png") }
            every { s.getChapterUrl(any()) } returns "https://source/c"
        }
    }

    @Test
    fun everySourceRunsAndFinishes() {
        val first = download(1L)
        val second = download(id = 2L, owner = manga.copy(id = 2L, source = 6L, ogTitle = "Else"), from = other)
        downloader.addAllToQueue(listOf(first, second))
        downloader.start()
        waitUntil { downloader.queueState.value.isEmpty() && !downloader.isRunning }
        listOf(first.status, second.status) shouldBe listOf(Download.State.DOWNLOADED, Download.State.DOWNLOADED)
    }

    @Test
    fun anErrorHandsOverToTheNext() {
        coEvery { source.getPageList(match { it.url == "/c/1" }) } throws IOException("gone")
        val failing = download(1L)
        val next = download(2L)
        downloader.addAllToQueue(listOf(failing, next))
        downloader.start()
        waitUntil { next.status == Download.State.DOWNLOADED && !downloader.isRunning }
        failing.status shouldBe Download.State.ERROR
        downloader.queueState.value shouldBe listOf(failing)
    }

    @Test
    fun parallelLimitHoldsSourcesBack() {
        provider.downloadPreferences.parallelSourceLimit.set(1)
        coEvery { source.getPageList(any()) } coAnswers { awaitCancellation() }
        val first = download(1L)
        val second = download(id = 2L, owner = manga.copy(id = 2L, source = 6L, ogTitle = "Else"), from = other)
        downloader.addAllToQueue(listOf(first, second))
        downloader.start()
        Thread.sleep(300)
        coVerify(exactly = 0) { other.getPageList(any()) }
        downloader.stop()
    }

    @Test
    fun removedDownloadIsCancelled() {
        coEvery { source.getPageList(any()) } coAnswers { awaitCancellation() }
        val hanging = download(1L)
        val next = download(2L)
        downloader.addAllToQueue(listOf(hanging, next))
        downloader.start()
        waitUntil { runCatching { coVerify { source.getPageList(any()) } }.isSuccess }
        coEvery { source.getPageList(any()) } returns readyPages(1)
        downloader.removeFromQueue(hanging)
        waitUntil { next.status == Download.State.DOWNLOADED && !downloader.isRunning }
        hanging.status shouldBe Download.State.NOT_DOWNLOADED
    }

    @Test
    fun runningDownloadKeepsItsJob() {
        coEvery { source.getPageList(any()) } coAnswers { awaitCancellation() }
        val hanging = download(1L)
        val quick = download(id = 2L, owner = manga.copy(id = 2L, source = 6L, ogTitle = "Else"), from = other)
        downloader.addAllToQueue(listOf(hanging, quick))
        downloader.start()
        waitUntil { downloader.queueState.value == listOf(hanging) }
        quick.status shouldBe Download.State.DOWNLOADED
        coVerify(exactly = 1) { source.getPageList(any()) }
        downloader.stop()
    }

    @Test
    fun startingTwiceKeepsOneJob() {
        coEvery { source.getPageList(any()) } coAnswers { awaitCancellation() }
        downloader.addAllToQueue(listOf(download(1L)))
        downloader.launchDownloaderJob()
        val job = downloader.downloaderJob
        downloader.launchDownloaderJob()
        downloader.downloaderJob shouldBe job
        downloader.stop()
    }

    @Test
    fun finishedQueueStartsNothing() {
        val done = download(1L)
        downloader.addAllToQueue(listOf(done))
        done.transition(Download.State.DOWNLOADED)
        downloader.launchDownloaderJob()
        Thread.sleep(300)
        coVerify(exactly = 0) { source.getPageList(any()) }
        downloader.stop()
    }
}
