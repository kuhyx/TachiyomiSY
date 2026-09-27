package eu.kanade.tachiyomi.data.download

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.util.storage.DiskUtil
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.verify
import kotlinx.coroutines.awaitCancellation
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class DownloadManagerQueueTest : DownloadManagerTestBase() {

    @Before
    fun stubDownloads() {
        mockkObject(DiskUtil)
        every { DiskUtil.getAvailableStorageSpace(any<UniFile>()) } returns (1L shl 40)
        coEvery { source.getPageList(any()) } coAnswers { awaitCancellation() }
    }

    private fun queue(vararg ids: Long) = manager.downloadChapters(manga, ids.map { chapter(it) }, autoStart = false)

    private fun ids() = manager.queueState.value.map { it.chapter.id }

    private fun jobScheduled() =
        verify { workManager.enqueueUniqueWork("Downloader", ExistingWorkPolicy.REPLACE, any<OneTimeWorkRequest>()) }

    @Test
    fun startAndStopPassThrough() {
        manager.downloaderStart() shouldBe false
        queue(1)
        manager.downloaderStart() shouldBe true
        manager.downloaderStop("paused by test")
        manager.isRunning shouldBe false
        manager.downloaderStop()
    }

    @Test
    fun startSchedulesTheJob() {
        queue(1)
        manager.startDownloads()
        jobScheduled()
        manager.isRunning shouldBe false
    }

    @Test
    fun runningJobStartsTheDownloader() {
        queue(1)
        running(true)
        manager.startDownloads()
        manager.isRunning shouldBe true
        manager.startDownloads()
        verify(exactly = 0) { workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>()) }
    }

    @Test
    fun pauseAndClear() {
        queue(1, 2)
        manager.downloaderStart()
        manager.pauseDownloads()
        manager.isRunning shouldBe false
        manager.clearQueue()
        ids() shouldBe emptyList()
    }

    @Test
    fun queuedLookup() {
        queue(1)
        manager.getQueuedDownloadOrNull(1L)?.chapter?.id shouldBe 1L
        manager.getQueuedDownloadOrNull(2L) shouldBe null
    }

    @Test
    fun startNowMovesToFront() {
        queue(1, 2)
        manager.startDownloadNow(2L)
        ids() shouldBe listOf(2L, 1L)
        jobScheduled()
    }

    @Test
    fun startNowFetchesUnqueued() {
        coEvery { getChapter.await(3L) } returns chapter(3)
        coEvery { getChapter.await(4L) } returns null
        coEvery { getManga.await(manga.id) } returns manga
        queue(1)
        manager.startDownloadNow(4L)
        ids() shouldBe listOf(1L)
        manager.startDownloadNow(3L)
        ids() shouldBe listOf(3L, 1L)
    }

    @Test
    fun frontOfQueueInsertions() {
        manager.addDownloadsToStartOfQueue(emptyList())
        queue(1)
        val extra = Download(source, manga, chapter(5))
        running(true)
        manager.addDownloadsToStartOfQueue(listOf(extra))
        ids() shouldBe listOf(5L, 1L)
        verify(exactly = 0) { workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>()) }
        running(false)
        manager.addDownloadsToStartOfQueue(listOf(Download(source, manga, chapter(6))))
        jobScheduled()
    }

    @Test
    fun cancelRemovesFromQueue() {
        queue(1, 2)
        manager.cancelQueuedDownloads(manager.queueState.value.take(1))
        ids() shouldBe listOf(2L)
    }
}
