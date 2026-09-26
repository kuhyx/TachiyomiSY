package eu.kanade.tachiyomi.data.download

import androidx.work.OneTimeWorkRequest
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.UnmeteredSource
import eu.kanade.tachiyomi.source.online.HttpSource
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/** [queueChapters]: what gets queued, and when the downloader starts or warns. */
@RunWith(RobolectricTestRunner::class)
internal class DownloaderQueueChaptersTest : DownloaderTestBase() {

    private fun queued(): List<Long> = downloader.queueState.value.map { it.chapter.id }

    private fun warned(): Boolean = shownNotification(Notifications.ID_DOWNLOAD_CHAPTER_ERROR) != null

    @Test
    fun nothingToQueue() {
        downloader.queueChapters(manga, emptyList(), autoStart = true)
        every { sourceManager.get(6L) } returns mockk<Source>()
        downloader.queueChapters(manga.copy(source = 6L), listOf(chapter(1L)), autoStart = true)
        queued() shouldBe emptyList()
        verify(exactly = 0) { workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>()) }
    }

    @Test
    fun newestSourceOrderFirst() {
        val chapters = listOf(chapter(id = 1L, order = 1L), chapter(id = 2L, order = 2L))
        downloader.queueChapters(manga, chapters, autoStart = false)
        queued() shouldBe listOf(2L, 1L)
        verify(exactly = 0) { workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>()) }
    }

    @Test
    fun downloadedAndQueuedAreSkipped() {
        File(root, "Source/Title/Ch 1").mkdirs()
        downloader.queueChapters(manga, listOf(chapter(2L)), autoStart = false)
        downloader.queueChapters(manga, listOf(chapter(1L), chapter(2L), chapter(3L)), autoStart = true)
        queued() shouldBe listOf(2L, 3L)
        downloader.queueChapters(manga, listOf(chapter(1L)), autoStart = true)
        queued() shouldBe listOf(2L, 3L)
    }

    @Test
    fun autoStartOnAnEmptyQueue() {
        downloader.queueChapters(manga, listOf(chapter(1L)), autoStart = true)
        verify { workManager.enqueueUniqueWork("Downloader", any(), any<OneTimeWorkRequest>()) }
        warned() shouldBe false
    }

    @Test
    fun manyChaptersFromOneSourceWarn() {
        downloader.queueChapters(manga, (1L..16L).map { chapter(it) }, autoStart = true)
        warned() shouldBe true
    }

    @Test
    fun manyChaptersOverallWarn() {
        downloader.queueChapters(manga, (1L..31L).map { chapter(it) }, autoStart = true)
        warned() shouldBe true
    }

    @Test
    fun unmeteredSourcesNeverWarn() {
        val local = mockk<HttpSource>(moreInterfaces = arrayOf(UnmeteredSource::class))
        every { local.id } returns 8L
        every { local.toString() } returns "Local"
        every { sourceManager.get(8L) } returns local
        downloader.queueChapters(manga.copy(source = 8L), (1L..40L).map { chapter(it) }, autoStart = true)
        warned() shouldBe false
        queued().size shouldBe 40
    }
}
