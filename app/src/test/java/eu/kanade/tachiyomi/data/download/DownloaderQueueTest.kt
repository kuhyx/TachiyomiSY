package eu.kanade.tachiyomi.data.download

import eu.kanade.tachiyomi.data.download.model.Download
import io.kotest.matchers.shouldBe
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class DownloaderQueueTest : DownloaderTestBase() {

    private fun queue(vararg downloads: Download): List<Download> {
        downloader.addAllToQueue(downloads.toList())
        return downloads.toList()
    }

    @Test
    fun addingQueuesAndPersists() {
        val (one, two) = queue(download(1L), download(2L))
        downloader.queueState.value shouldBe listOf(one, two)
        one.status shouldBe Download.State.QUEUE
        newDownloader().store.restore().map { it.chapter.id } shouldBe emptyList()
    }

    @Test
    fun removingResetsActiveDownloads() {
        val (queued, downloading, done) = queue(download(1L), download(2L), download(3L))
        downloading.transition(Download.State.DOWNLOADING)
        done.transition(Download.State.DOWNLOADED)
        downloader.removeFromQueue(queued)
        downloader.removeFromQueue(downloading)
        downloader.removeFromQueue(done)
        listOf(queued.status, downloading.status, done.status) shouldBe
            listOf(Download.State.NOT_DOWNLOADED, Download.State.NOT_DOWNLOADED, Download.State.DOWNLOADED)
        downloader.queueState.value shouldBe emptyList()
    }

    @Test
    fun removingByChapterAndManga() {
        val other = manga.copy(id = 9L)
        val one = download(1L)
        val two = download(2L)
        val three = download(3L, owner = other)
        val four = download(4L)
        queue(one, two, three, four)
        two.transition(Download.State.DOWNLOADING)
        four.transition(Download.State.ERROR)
        downloader.removeFromQueue(listOf(one.chapter, two.chapter, four.chapter))
        two.status shouldBe Download.State.NOT_DOWNLOADED
        four.status shouldBe Download.State.ERROR
        downloader.removeFromQueue(other)
        downloader.queueState.value shouldBe emptyList()
    }

    @Test
    fun clearingResetsActiveOnly() {
        val (queued, downloading, failed) = queue(download(1L), download(2L), download(3L))
        downloading.transition(Download.State.DOWNLOADING)
        failed.transition(Download.State.ERROR)
        downloader.clearQueue()
        listOf(queued.status, downloading.status, failed.status) shouldBe
            listOf(Download.State.NOT_DOWNLOADED, Download.State.NOT_DOWNLOADED, Download.State.ERROR)
    }

    @Test
    fun finishedMeansDoneOrFailed() {
        val (one, two) = queue(download(1L), download(2L))
        downloader.areAllDownloadsFinished() shouldBe false
        one.transition(Download.State.DOWNLOADED)
        two.transition(Download.State.DOWNLOADING)
        downloader.areAllDownloadsFinished() shouldBe false
        two.transition(Download.State.ERROR)
        downloader.areAllDownloadsFinished() shouldBe true
    }

    @Test
    fun emptyUpdateClearsAndStops() {
        queue(download(1L))
        downloader.updateQueue(emptyList())
        downloader.queueState.value shouldBe emptyList()
        verify { workManager.cancelUniqueWork("Downloader") }
    }

    @Test
    fun updateReplacesTheQueue() {
        queue(download(1L), download(2L))
        downloader.updateQueue(listOf(download(2L), download(1L)))
        downloader.queueState.value.map { it.chapter.id } shouldBe listOf(2L, 1L)
        downloader.isRunning shouldBe false
    }
}
