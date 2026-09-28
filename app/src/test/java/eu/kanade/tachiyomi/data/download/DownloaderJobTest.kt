package eu.kanade.tachiyomi.data.download

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.UnmeteredSource
import eu.kanade.tachiyomi.source.online.HttpSource
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/** Queueing chapters and the job that runs the queue. */
@RunWith(RobolectricTestRunner::class)
internal class DownloaderJobTest : DownloaderPipelineBase() {

    private fun chapters(vararg ids: Long) = ids.map { chapter(it) }

    private fun queuedIds() = downloader.queueState.value.map { it.chapter.id }

    @Test
    fun nothingToQueue() {
        downloader.queueChapters(manga, emptyList(), autoStart = true)
        every { sourceManager.get(5L) } returns mockk<Source>()
        downloader.queueChapters(manga, chapters(1), autoStart = true)
        queuedIds() shouldBe emptyList()
    }

    @Test
    fun queueSkipsDownloadedAndQueued() {
        File(root, "Source/Title/Ch 1").mkdirs()
        downloader.queueChapters(manga, chapters(1, 2, 3), autoStart = false)
        downloader.queueChapters(manga, chapters(2, 4), autoStart = false)
        downloader.queueChapters(manga, chapters(4), autoStart = true)
        queuedIds() shouldBe listOf(3L, 2L, 4L)
        verify(exactly = 0) { workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>()) }
    }

    @Test
    fun autoStartSchedulesTheJob() {
        downloader.queueChapters(manga, chapters(1), autoStart = true)
        verify { workManager.enqueueUniqueWork("Downloader", ExistingWorkPolicy.REPLACE, any<OneTimeWorkRequest>()) }
        shownNotification(WARNING_ID) shouldBe null
    }

    @Test
    fun manyFromOneSourceWarns() {
        downloader.queueChapters(manga, (1L..16L).map { chapter(it) }, autoStart = true)
        (shownNotification(WARNING_ID) != null) shouldBe true
    }

    @Test
    fun hugeQueueWarns() {
        downloader.queueChapters(manga, (1L..31L).map { chapter(it) }, autoStart = true)
        (shownNotification(WARNING_ID) != null) shouldBe true
    }

    @Test
    fun unmeteredSourcesNeverWarn() {
        val free = mockk<HttpSource>(null, false, UnmeteredSource::class)
        every { free.toString() } returns "Free"
        every { free.id } returns 5L
        every { free.name } returns "Free"
        every { sourceManager.get(5L) } returns free
        downloader.queueChapters(manga, (1L..40L).map { chapter(it) }, autoStart = true)
        shownNotification(WARNING_ID) shouldBe null
    }

    @Test
    fun jobDrainsTheQueue() {
        downloader.queueChapters(manga, chapters(1, 2), autoStart = false)
        downloader.start() shouldBe true
        downloader.launchDownloaderJob()
        waitUntil { downloader.queueState.value.isEmpty() }
        verify(timeout = 5_000) { workManager.cancelUniqueWork("Downloader") }
    }

    @Test
    fun errorsLetTheNextOneRun() {
        // Chapters queue by descending source order: chapter 2 runs first and fails, then chapter 1 finishes.
        coEvery { source.getPageList(match { it.url == "/c/2" }) } returns emptyList()
        downloader.queueChapters(manga, chapters(1, 2), autoStart = false)
        downloader.start()
        waitUntil { queuedIds() == listOf(2L) }
        downloader.queueState.value.single().status shouldBe Download.State.ERROR
        // The finished job stops the downloader last; wait for it so nothing runs past the test.
        verify(timeout = 5_000) { workManager.cancelUniqueWork("Downloader") }
    }

    @Test
    fun removedDownloadIsCancelled() {
        coEvery { source.getPageList(match { it.url == "/c/1" }) } coAnswers { awaitCancellation() }
        downloader.queueChapters(manga, chapters(1), autoStart = false)
        downloader.start()
        waitUntil { downloader.isRunning }
        downloader.queueChapters(manga, chapters(2), autoStart = false)
        downloader.removeFromQueue(downloader.queueState.value.first())
        waitUntil { downloader.queueState.value.isEmpty() }
        verify(timeout = 5_000) { workManager.cancelUniqueWork("Downloader") }
    }

    @Test
    fun brokenDirectoryStopsTheJob() {
        // A plain file where the temporary chapter directory goes makes createDirectory fail.
        File(root, "Source/Title").mkdirs()
        File(root, "Source/Title/${tempName("Ch 1")}").writeText("in the way")
        runBlocking { with(downloader) { scope.launchDownloadJob(download(1L)).join() } }
        verify { workManager.cancelUniqueWork("Downloader") }
    }

    private companion object {
        const val WARNING_ID = -202
    }
}
