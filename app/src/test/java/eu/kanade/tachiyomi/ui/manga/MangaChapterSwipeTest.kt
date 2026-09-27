package eu.kanade.tachiyomi.ui.manga

import eu.kanade.domain.track.model.AutoTrackState
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.cancelQueuedDownloads
import eu.kanade.tachiyomi.data.download.deleteChapters
import eu.kanade.tachiyomi.data.download.getQueuedDownloadOrNull
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.download.startDownloadNow
import eu.kanade.tachiyomi.data.track.Tracker
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import logcat.LogPriority
import logcat.LogcatLogger
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.chapter.model.ChapterUpdate
import tachiyomi.domain.library.service.LibraryPreferences.ChapterSwipeAction
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.CopyOnWriteArrayList

private const val SWIPE_QUEUE = "eu.kanade.tachiyomi.data.download.DownloadManagerQueueKt"
private const val SWIPE_DELETION = "eu.kanade.tachiyomi.data.download.DownloadManagerDeletionKt"

/** What a swipe on a chapter row does for each configured swipe action. */
@RunWith(RobolectricTestRunner::class)
internal class MangaChapterSwipeTest {
    private val harness = MangaHarness()
    private val parts get() = harness.parts

    @Before
    fun setUp() {
        harness.start()
        harness.mangaFlow.value = manga(favorite = true) to listOf(chapter(1L), chapter(2L))
    }

    @After
    fun tearDown() {
        unmockkStatic(SWIPE_QUEUE, SWIPE_DELETION)
        harness.stop()
    }

    // The download manager's queue and deletion helpers are extensions; stub them to see what a swipe asks for.
    private fun stubDownloads() {
        mockkStatic(SWIPE_QUEUE, SWIPE_DELETION)
        every { any<DownloadManager>().startDownloadNow(any()) } just runs
        every { any<DownloadManager>().getQueuedDownloadOrNull(any()) } returns mockk(relaxed = true)
        every { any<DownloadManager>().cancelQueuedDownloads(any()) } just runs
        every { any<DownloadManager>().deleteChapters(any(), any(), any()) } just runs
    }

    @Test
    fun toggleReadFlipsTheMark() {
        val model = harness.loaded()
        model.chapterActions.chapterSwipe(item(chapter(1L, read = true)), ChapterSwipeAction.ToggleRead)
        coVerify(timeout = 5_000) { parts.setReadStatus.await(read = false, chapters = anyVararg()) }
    }

    @Test
    fun toggleBookmarkFlipsTheMark() {
        val model = harness.loaded()
        model.chapterActions.chapterSwipe(item(chapter(1L, bookmark = true)), ChapterSwipeAction.ToggleBookmark)
        coVerify(timeout = 5_000) { parts.updateChapter.awaitAll(listOf(ChapterUpdate(id = 1L, bookmark = false))) }
    }

    @Test
    fun downloadStartsFailedOnes() {
        stubDownloads()
        val model = harness.loaded()
        model.chapterActions.chapterSwipe(item(chapter(1L), Download.State.ERROR), ChapterSwipeAction.Download)
        verify(timeout = 5_000) { harness.downloadManager.startDownloadNow(1L) }
        model.chapterActions.chapterSwipe(item(chapter(2L)), ChapterSwipeAction.Download)
        verify(timeout = 5_000) { harness.downloadManager.startDownloadNow(2L) }
    }

    @Test
    fun downloadCancelsQueuedOnes() {
        stubDownloads()
        val model = harness.loaded()
        model.chapterActions.chapterSwipe(item(chapter(1L), Download.State.QUEUE), ChapterSwipeAction.Download)
        model.chapterActions.chapterSwipe(item(chapter(2L), Download.State.DOWNLOADING), ChapterSwipeAction.Download)
        eventually { true }
        verify(timeout = 5_000, exactly = 2) { harness.downloadManager.cancelQueuedDownloads(any()) }
    }

    @Test
    fun downloadDeletesDownloaded() {
        stubDownloads()
        val model = harness.loaded()
        model.chapterActions.chapterSwipe(item(chapter(1L), Download.State.DOWNLOADED), ChapterSwipeAction.Download)
        eventually { true }
        verify(timeout = 5_000) { harness.downloadManager.deleteChapters(any(), any(), any()) }
    }

    @Test
    fun failedDeletionIsSwallowed() {
        stubDownloads()
        every { any<DownloadManager>().deleteChapters(any(), any(), any()) } throws IllegalStateException("disk")
        val model = harness.loaded()
        model.chapterActions.chapterSwipe(item(chapter(1L), Download.State.DOWNLOADED), ChapterSwipeAction.Download)
        eventually { true }
        verify(timeout = 5_000) { harness.downloadManager.deleteChapters(any(), any(), any()) }
        model.state.value.shouldBeInstanceOf<MangaScreenModel.State.Success>()
    }

    // Launched, the error would crash the model's coroutine and leak into kotlinx-coroutines-test's global
    // exception collector; run the swipe body directly instead.
    @Test
    fun disabledSwipeIsAnError() {
        val model = harness.loaded()
        val body = MangaChapterActions::class.java.getDeclaredMethod(
            "executeChapterSwipeAction",
            ChapterList.Item::class.java,
            ChapterSwipeAction::class.java,
        )
        body.isAccessible = true
        val thrown = shouldThrow<InvocationTargetException> {
            body.invoke(model.chapterActions, item(chapter(1L)), ChapterSwipeAction.Disabled)
        }
        thrown.cause.shouldBeInstanceOf<IllegalStateException>()
    }

    @Test
    fun refreshErrorsAreLogged() {
        val logged = CopyOnWriteArrayList<String>()
        val logger = object : LogcatLogger {
            override fun isLoggable(priority: LogPriority, tag: String): Boolean = true

            override fun log(priority: LogPriority, tag: String, message: String) {
                logged += message
            }
        }
        LogcatLogger.install()
        LogcatLogger.loggers += logger
        try {
            val model = harness.loaded()
            model.updateSuccessState { it.copy(hasLoggedInTrackers = true) }
            model.autoTrackState = AutoTrackState.ALWAYS
            coEvery { parts.getTracks.await(1L) } returns emptyList()
            val failing = mockk<Tracker> {
                every { id } returns 5L
                every { name } returns "Broken"
            }
            coEvery { parts.refreshTracks.await(1L) } returns listOf(failing to IllegalStateException("down"))
            model.chapterActions.markChaptersRead(listOf(chapter(2L)), read = true)
            eventually { logged.any { it.startsWith("Failed to refresh track data") } }
        } finally {
            LogcatLogger.loggers -= logger
            LogcatLogger.uninstall()
        }
        logged.any { it.contains("for service 5") } shouldBe true
    }
}
