package eu.kanade.tachiyomi.data.download

import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.notification.Notifications
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.verify
import kotlinx.coroutines.awaitCancellation
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class DownloaderStateTest : DownloaderTestBase() {

    @Before
    fun hangDownloads() {
        coEvery { source.getPageList(any()) } coAnswers { awaitCancellation() }
    }

    @Test
    fun emptyQueueDoesNotStart() {
        downloader.start() shouldBe false
        downloader.isRunning shouldBe false
    }

    @Test
    fun startRequeuesPendingDownloads() {
        val (done, failed, queued) = listOf(download(1L), download(2L), download(3L))
        downloader.addAllToQueue(listOf(done, failed, queued))
        done.transition(Download.State.DOWNLOADED)
        failed.transition(Download.State.ERROR)
        downloader.start() shouldBe true
        failed.status shouldBe Download.State.QUEUE
        done.status shouldBe Download.State.DOWNLOADED
        downloader.isRunning shouldBe true
        downloader.start() shouldBe false
    }

    @Test
    fun startWithNothingPending() {
        val done = download(1L)
        downloader.addAllToQueue(listOf(done))
        done.transition(Download.State.DOWNLOADED)
        downloader.start() shouldBe false
    }

    @Test
    fun pauseKeepsTheQueue() {
        val active = download(1L)
        downloader.addAllToQueue(listOf(active))
        downloader.start()
        active.transition(Download.State.DOWNLOADING)
        downloader.pause()
        active.status shouldBe Download.State.QUEUE
        downloader.isPaused shouldBe true
        downloader.stop()
        shownNotification(Notifications.ID_DOWNLOAD_CHAPTER_PROGRESS)!!.actions.size shouldBe 2
        downloader.isPaused shouldBe false
    }

    @Test
    fun stopFailsActiveDownloads() {
        val active = download(1L)
        downloader.addAllToQueue(listOf(active))
        active.transition(Download.State.DOWNLOADING)
        downloader.stop()
        active.status shouldBe Download.State.ERROR
        verify { workManager.cancelUniqueWork("Downloader") }
    }

    @Test
    fun stopWithAReasonWarns() {
        downloader.stop(reason = "no wifi")
        shownNotification(Notifications.ID_DOWNLOAD_CHAPTER_ERROR) shouldBe
            shownNotification(Notifications.ID_DOWNLOAD_CHAPTER_ERROR)!!
        verify(exactly = 0) { workManager.cancelUniqueWork(any()) }
    }

    @Test
    fun pausedEmptyQueueCompletes() {
        downloader.isPaused = true
        downloader.stop()
        shownNotification(Notifications.ID_DOWNLOAD_CHAPTER_PROGRESS) shouldBe null
    }

    @Test
    fun updateRestartsARunningQueue() {
        downloader.addAllToQueue(listOf(download(1L)))
        downloader.start()
        downloader.updateQueue(listOf(download(2L)))
        downloader.isRunning shouldBe true
        downloader.queueState.value.map { it.chapter.id } shouldBe listOf(2L)
    }
}
