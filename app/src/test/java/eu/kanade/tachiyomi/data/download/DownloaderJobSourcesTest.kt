package eu.kanade.tachiyomi.data.download

import androidx.work.OneTimeWorkRequest
import eu.kanade.tachiyomi.data.download.model.Download
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.awaitCancellation
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The job across sources: a busy queue does not restart it, and one source's failure leaves another running. */
@RunWith(RobolectricTestRunner::class)
internal class DownloaderJobSourcesTest : DownloaderPipelineBase() {

    private val other = httpSource(name = "Other", id = 6L)
    private val otherManga by lazy { manga.copy(id = 2L, source = 6L, ogTitle = "Second") }

    @Test
    fun busyQueueDoesNotReschedule() {
        downloader.queueChapters(manga, listOf(chapter(1)), autoStart = false)
        downloader.queueChapters(manga, listOf(chapter(2)), autoStart = true)
        downloader.queueState.value.size shouldBe 2
        verify(exactly = 0) { workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>()) }
    }

    @Test
    fun otherSourceKeepsItsJob() {
        every { sourceManager.get(6L) } returns other
        coEvery { source.getPageList(any()) } coAnswers { awaitCancellation() }
        // Chapters queue by descending source order, so chapter 4 runs (and fails) before chapter 3.
        coEvery { other.getPageList(match { it.url == "/c/4" }) } returns emptyList()
        coEvery { other.getPageList(match { it.url == "/c/3" }) } coAnswers { awaitCancellation() }
        downloader.queueChapters(manga, listOf(chapter(1)), autoStart = false)
        downloader.queueChapters(otherManga, listOf(chapter(3), chapter(4)), autoStart = false)
        downloader.start()
        waitUntil { downloader.queueState.value.any { it.status == Download.State.ERROR } }
        coVerify(timeout = 5_000) { other.getPageList(match { it.url == "/c/3" }) }
        downloader.queueState.value.first().status shouldBe Download.State.QUEUE
    }

    @Test
    fun finishedQueueIdles() {
        // Only finished downloads queued: the job starts, finds nothing active and waits for the queue to change.
        val finished = spyk(download(1L))
        downloader.addAllToQueue(listOf(finished))
        finished.transition(Download.State.DOWNLOADED)
        clearMocks(finished, answers = false)
        // start() reads the status once; the job's first pass reads it again.
        downloader.start() shouldBe false
        verify(timeout = 5_000, atLeast = 2) { finished.status }
        downloader.isRunning shouldBe true
    }
}
