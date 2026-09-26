package eu.kanade.tachiyomi.data.download

import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import com.google.common.util.concurrent.Futures.immediateFuture
import eu.kanade.tachiyomi.data.download.model.Download
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.awaitCancellation
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class DownloadManagerQueueTest : DownloadManagerTestBase() {

    private fun jobRunning(running: Boolean) {
        val info = mockk<WorkInfo> { every { state } returns WorkInfo.State.RUNNING }
        every { workManager.getWorkInfosForUniqueWork("Downloader") } returns
            immediateFuture(if (running) listOf(info) else emptyList())
    }

    private fun ids(): List<Long> = manager.queueState.value.map { it.chapter.id }

    @Before
    fun hangDownloads() {
        coEvery { source.getPageList(any()) } coAnswers { awaitCancellation() }
    }

    @Test
    fun startEnqueuesTheJob() {
        manager.startDownloads()
        verify { workManager.enqueueUniqueWork("Downloader", any(), any<OneTimeWorkRequest>()) }
    }

    @Test
    fun runningJobStartsDownloader() {
        jobRunning(true)
        manager.downloader.addAllToQueue(listOf(download(1L)))
        manager.startDownloads()
        manager.isRunning shouldBe true
        manager.startDownloads()
        verify(exactly = 0) { workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>()) }
        manager.downloaderStop()
    }

    @Test
    fun serviceStartAndStop() {
        manager.downloader.addAllToQueue(listOf(download(1L)))
        manager.downloaderStart() shouldBe true
        manager.downloaderStop(reason = "no network")
        manager.isRunning shouldBe false
    }

    @Test
    fun pauseAndClear() {
        manager.downloader.addAllToQueue(listOf(download(1L)))
        manager.pauseDownloads()
        ids() shouldBe listOf(1L)
        manager.clearQueue()
        ids() shouldBe emptyList()
    }

    @Test
    fun startNowMovesToTheFront() {
        manager.downloader.addAllToQueue(listOf(download(1L), download(2L)))
        manager.getQueuedDownloadOrNull(2L)!!.chapter.id shouldBe 2L
        manager.getQueuedDownloadOrNull(9L) shouldBe null
        manager.startDownloadNow(2L)
        ids() shouldBe listOf(2L, 1L)
    }

    @Test
    fun startNowFetchesUnqueued() {
        coEvery { getChapter.await(3L) } returns chapter(3L)
        coEvery { getManga.await(1L) } returns manga
        manager.startDownloadNow(3L)
        ids() shouldBe listOf(3L)
        coEvery { getChapter.await(4L) } returns null
        manager.startDownloadNow(4L)
        ids() shouldBe listOf(3L)
    }

    @Test
    fun addingToTheFront() {
        manager.addDownloadsToStartOfQueue(emptyList())
        manager.downloader.addAllToQueue(listOf(download(1L)))
        manager.addDownloadsToStartOfQueue(listOf(download(2L)))
        ids() shouldBe listOf(2L, 1L)
        verify { workManager.enqueueUniqueWork("Downloader", any(), any<OneTimeWorkRequest>()) }
        jobRunning(true)
        manager.addDownloadsToStartOfQueue(listOf(download(3L)))
        ids() shouldBe listOf(3L, 2L, 1L)
        manager.downloaderStop()
    }

    @Test
    fun cancellingQueuedDownloads() {
        manager.downloader.addAllToQueue(listOf(download(1L), download(2L)))
        val (first, _) = manager.queueState.value
        manager.cancelQueuedDownloads(listOf(first))
        ids() shouldBe listOf(2L)
        first.status shouldBe Download.State.NOT_DOWNLOADED
    }

    @Test
    fun twoRunningJobsAreNotRunning() {
        val info = mockk<WorkInfo> { every { state } returns WorkInfo.State.RUNNING }
        every { workManager.getWorkInfosForUniqueWork("Downloader") } returns immediateFuture(listOf(info, info))
        DownloadJob.isRunning(context) shouldBe false
    }
}
