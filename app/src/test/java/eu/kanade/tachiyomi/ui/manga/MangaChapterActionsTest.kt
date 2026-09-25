package eu.kanade.tachiyomi.ui.manga

import eu.kanade.domain.track.model.AutoTrackState
import eu.kanade.tachiyomi.data.track.Tracker
import eu.kanade.tachiyomi.data.track.domainTrack
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowToast
import tachiyomi.domain.chapter.model.ChapterUpdate
import tachiyomi.domain.manga.model.Manga

@RunWith(RobolectricTestRunner::class)
internal class MangaChapterActionsTest {
    private val harness = MangaHarness()
    private val parts get() = harness.parts

    @Before
    fun setUp() {
        harness.start()
        harness.mangaFlow.value = manga(favorite = true) to listOf(chapter(1L), chapter(2L), chapter(3L))
    }

    @After
    fun tearDown() = harness.stop()

    private fun tracked(model: MangaScreenModel, autoTrack: AutoTrackState) {
        model.updateSuccessState { it.copy(hasLoggedInTrackers = true) }
        model.autoTrackState = autoTrack
        coEvery { parts.getTracks.await(1L) } returns listOf(
            domainTrack(id = 1L).copy(lastChapterRead = 1.0),
        )
    }

    @Test
    fun emptyReadMarkDoesNothing() {
        harness.loaded().chapterActions.markChaptersRead(emptyList(), read = true)
        coVerify(exactly = 0) { parts.setReadStatus.await(read = any(), chapters = anyVararg()) }
    }

    @Test
    fun readWithoutTrackersOnlyMarks() {
        val model = harness.loaded()
        model.chapterActions.markChaptersRead(listOf(chapter(2L)), read = true)
        coVerify(timeout = 5_000) { parts.setReadStatus.await(read = true, chapters = anyVararg()) }
        coVerify(exactly = 0) { parts.getTracks.await(any<Long>()) }
    }

    @Test
    fun alwaysAutoTrackUpdates() {
        val model = harness.loaded()
        tracked(model, AutoTrackState.ALWAYS)
        val failing = mockk<Tracker> {
            every { id } returns 5L
            every { name } returns "Broken"
        }
        coEvery { parts.refreshTracks.await(1L) } returns listOf(
            null to IllegalStateException("skip"),
            failing to IllegalStateException("down"),
            failing to IllegalStateException(),
        )
        model.chapterActions.markChaptersRead(listOf(chapter(2L), chapter(3L)), read = true)
        coVerify(timeout = 5_000) { parts.trackChapter.await(any(), 1L, 3.0) }
        eventually { ShadowToast.shownToastCount() == 3 }
    }

    @Test
    fun askAutoTrackWaitsForOk() {
        val model = harness.loaded()
        tracked(model, AutoTrackState.ASK)
        model.chapterActions.markChaptersRead(listOf(chapter(2L)), read = true)
        eventually { model.snackbarHostState.currentSnackbarData != null }
        model.snackbarHostState.currentSnackbarData?.performAction()
        coVerify(timeout = 5_000) { parts.trackChapter.await(any(), 1L, 2.0) }
        model.chapterActions.markChaptersRead(listOf(chapter(3L)), read = true)
        eventually { model.snackbarHostState.currentSnackbarData != null }
        model.snackbarHostState.currentSnackbarData?.dismiss()
        coVerify(exactly = 0) { parts.trackChapter.await(any(), 1L, 3.0) }
    }

    @Test
    fun trackAlreadyAheadIsNotPrompted() {
        val model = harness.loaded()
        tracked(model, AutoTrackState.ALWAYS)
        model.chapterActions.markChaptersRead(listOf(chapter(1L)), read = true)
        coVerify(timeout = 5_000) { parts.getTracks.await(1L) }
        coVerify(exactly = 0) { parts.trackChapter.await(any(), any(), any()) }
    }

    @Test
    fun neverAndUnreadSkipTracking() {
        val model = harness.loaded()
        tracked(model, AutoTrackState.NEVER)
        model.chapterActions.markChaptersRead(listOf(chapter(2L)), read = true)
        model.autoTrackState = AutoTrackState.ALWAYS
        model.chapterActions.markChaptersRead(listOf(chapter(2L)), read = false)
        coVerify(timeout = 5_000, exactly = 2) { parts.setReadStatus.await(read = any(), chapters = anyVararg()) }
        coVerify(exactly = 0) { parts.getTracks.await(any<Long>()) }
    }

    @Test
    fun previousChaptersAreMarked() {
        val model = harness.loaded()
        model.chapterActions.markPreviousChapterRead(chapter(3L))
        coVerify(timeout = 5_000) {
            parts.setReadStatus.await(read = true, chapters = arrayOf(chapter(1L), chapter(2L)))
        }
        model.chapterActions.markPreviousChapterRead(chapter(9L))
        harness.mangaFlow.value = manga(flags = Manga.CHAPTER_SORT_ASC, favorite = true) to
            listOf(chapter(1L), chapter(2L))
        model.awaitSuccess { it.manga.chapterFlags == Manga.CHAPTER_SORT_ASC }
        model.chapterActions.markPreviousChapterRead(chapter(2L))
        coVerify(timeout = 5_000) { parts.setReadStatus.await(read = true, chapters = arrayOf(chapter(1L))) }
    }

    @Test
    fun loadingStateMarksNothing() {
        val model = harness.loading()
        model.chapterActions.markPreviousChapterRead(chapter(3L))
        coVerify(exactly = 0) { parts.setReadStatus.await(read = any(), chapters = anyVararg()) }
    }

    @Test
    fun bookmarksOnlyChangedChapters() {
        val model = harness.loaded()
        model.toggleSelection(model.awaitSuccess().chapters.first(), selected = true)
        model.chapterActions.bookmarkChapters(listOf(chapter(1L), chapter(2L, bookmark = true)), bookmarked = true)
        coVerify(timeout = 5_000) { parts.updateChapter.awaitAll(listOf(ChapterUpdate(id = 1L, bookmark = true))) }
        model.awaitSuccess().isAnySelected shouldBe false
    }
}
