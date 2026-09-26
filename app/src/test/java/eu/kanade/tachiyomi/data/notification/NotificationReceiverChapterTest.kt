package eu.kanade.tachiyomi.data.notification

import android.app.Application
import eu.kanade.tachiyomi.data.download.deleteChapters
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.ui.manga.DELETION
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowToast
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga

@RunWith(RobolectricTestRunner::class)
internal class NotificationReceiverChapterTest : ReceiverTestBase() {

    private val manga = Manga.create().copy(id = 3L, source = 8L)
    private val chapter = Chapter.create().copy(id = 4L, mangaId = 3L, url = "/c")

    private fun openChapter() = receive(receiverAction("ACTION_OPEN_CHAPTER")) {
        putExtra(NotificationReceiver.EXTRA_MANGA_ID, 3L)
        putExtra(receiverExtra("EXTRA_CHAPTER_ID"), 4L)
    }

    private fun chapterAction(action: String, mangaId: Long = 3L, notificationId: Int = -1) = receive(action) {
        putExtra(NotificationReceiver.EXTRA_MANGA_ID, mangaId)
        putExtra(NotificationReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        putExtra(NotificationReceiver.EXTRA_CHAPTER_URL, arrayOf("/c", "/gone"))
    }

    @Test
    fun openChapterStartsTheReader() {
        coEvery { getManga.await(3L) } returns manga
        coEvery { getChapter.await(4L) } returns chapter
        openChapter()
        val started = Shadows.shadowOf(context as Application).nextStartedActivity
        started.component!!.className shouldBe "eu.kanade.tachiyomi.ui.reader.ReaderActivity"
    }

    @Test
    fun openMissingChapterToasts() {
        coEvery { getManga.await(3L) } returns manga
        coEvery { getChapter.await(4L) } returns null
        openChapter()
        coEvery { getManga.await(3L) } returns null
        openChapter()
        ShadowToast.shownToastCount() shouldBe 2
    }

    @Test
    fun downloadQueuesFoundChapters() {
        coEvery { getManga.await(3L) } returns manga
        coEvery { getChapter.await("/c", 3L) } returns chapter
        coEvery { getChapter.await("/gone", 3L) } returns null
        postNotification(context = context, id = 6)
        chapterAction(action = NotificationReceiver.ACTION_DOWNLOAD_CHAPTER, notificationId = 6)
        verify(timeout = 5_000) { downloadManager.downloadChapters(manga, listOf(chapter)) }
        activeIds(context) shouldBe emptyList()
    }

    @Test
    fun downloadSkipsMissingManga() {
        coEvery { getManga.await(3L) } returns null
        chapterAction(NotificationReceiver.ACTION_DOWNLOAD_CHAPTER)
        coVerify(timeout = 5_000) { getManga.await(3L) }
        verify(exactly = 0) { downloadManager.downloadChapters(any(), any(), any()) }
    }

    @Test
    fun actionsNeedUrlsAndManga() {
        receive(NotificationReceiver.ACTION_MARK_AS_READ)
        chapterAction(action = NotificationReceiver.ACTION_MARK_AS_READ, mangaId = -1)
        coVerify(exactly = 0) { getChapter.await(any<String>(), any()) }
    }

    @Test
    fun markAsReadKeepsDownloads() {
        coEvery { getChapter.await("/c", 3L) } returns chapter
        coEvery { getChapter.await("/gone", 3L) } returns null
        chapterAction(NotificationReceiver.ACTION_MARK_AS_READ)
        coVerify(timeout = 5_000) { updateChapter.awaitAll(match { it.single().read == true }) }
        coVerify(exactly = 0) { getManga.await(any<Long>()) }
    }

    /** The deletion is an extension, which the relaxed manager mock does not intercept: stub it statically. */
    @Test
    fun markAsReadDeletesDownloads() {
        mockkStatic(DELETION)
        every { downloadManager.deleteChapters(any(), any(), any()) } just runs
        downloadPreferences.removeAfterMarkedAsRead.set(true)
        val source = mockk<Source>()
        coEvery { getChapter.await("/c", 3L) } returns chapter
        coEvery { getChapter.await("/gone", 3L) } returns null
        coEvery { getManga.await(3L) } returns manga
        every { sourceManager.get(8L) } returns source
        chapterAction(NotificationReceiver.ACTION_MARK_AS_READ)
        coVerify(timeout = 5_000) { updateChapter.awaitAll(any()) }
        verify { downloadManager.deleteChapters(listOf(chapter), manga, source) }
    }

    @Test
    fun markAsReadWithoutSource() {
        downloadPreferences.removeAfterMarkedAsRead.set(true)
        coEvery { getChapter.await("/c", 3L) } returns chapter
        coEvery { getChapter.await("/gone", 3L) } returns null
        coEvery { getManga.await(3L) } returns manga
        every { sourceManager.get(8L) } returns null
        chapterAction(NotificationReceiver.ACTION_MARK_AS_READ)
        coVerify(timeout = 5_000) { updateChapter.awaitAll(any()) }
        coEvery { getManga.await(3L) } returns null
        chapterAction(NotificationReceiver.ACTION_MARK_AS_READ)
        coVerify(timeout = 5_000, exactly = 2) { updateChapter.awaitAll(any()) }
    }
}
