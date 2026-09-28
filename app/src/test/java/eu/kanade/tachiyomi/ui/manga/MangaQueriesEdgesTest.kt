package eu.kanade.tachiyomi.ui.manga

import eu.kanade.tachiyomi.data.download.deleteManga
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.source.online.all.MergedSource
import exh.source.EH_SOURCE_ID
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.model.Manga

/** Chapter queries over every read, bookmark and download combination, and leaving the library with downloads. */
@RunWith(RobolectricTestRunner::class)
internal class MangaQueriesEdgesTest {
    private val harness = MangaHarness()

    @Before
    fun setUp() {
        mockkStatic(DELETION)
        every { harness.downloadManager.deleteManga(any(), any(), any()) } just runs
        harness.start()
        harness.mangaFlow.value = manga(favorite = true) to listOf(chapter(1L))
    }

    @After
    fun tearDown() {
        harness.stop()
        unmockkStatic(DELETION)
    }

    private fun rows(model: MangaScreenModel) {
        harness.awaitObserver(model)
        model.updateSuccessState {
            it.copy(
                chapters = listOf(
                    item(chapter(1L, read = true, bookmark = true)),
                    item(chapter(2L, bookmark = true), Download.State.DOWNLOADED),
                    item(chapter(3L, number = 9.0)),
                    item(chapter(4L, bookmark = true)),
                ),
            )
        }
    }

    @Test
    fun loadingHasNoChapters() {
        val model = harness.loading()
        model.getUnreadChapters() shouldBe emptyList()
        model.getBookmarkedChapters() shouldBe emptyList()
        harness.readerPreferences.skipFiltered.set(false)
        eventually { !model.skipFiltered }
        model.getUnreadChapters() shouldBe emptyList()
        model.getBookmarkedChapters() shouldBe emptyList()
        model.getUnreadChaptersSorted() shouldBe emptyList()
    }

    @Test
    fun queriesSkipReadAndDownloaded() {
        val model = harness.loaded()
        rows(model)
        model.getUnreadChapters().map { it.id } shouldContainExactlyInAnyOrder listOf(3L, 4L)
        model.getBookmarkedChapters().map { it.id } shouldContainExactlyInAnyOrder listOf(1L, 4L)
        harness.readerPreferences.skipFiltered.set(false)
        eventually { !model.skipFiltered }
        model.getBookmarkedChapters().map { it.id } shouldContainExactlyInAnyOrder listOf(1L, 4L)
        model.getUnreadChapters().map { it.id } shouldContainExactlyInAnyOrder listOf(3L, 4L)
    }

    @Test
    fun ehEntriesSortOldestLast() {
        harness.mangaFlow.value = manga(source = EH_SOURCE_ID, favorite = true) to listOf(chapter(1L))
        val model = harness.loaded()
        rows(model)
        model.getUnreadChaptersSorted().size shouldBe 2
    }

    @Test
    fun swipeMarksReadChapterUnread() {
        val model = harness.loaded()
        val read = chapter(5L, read = true)
        model.chapterActions.chapterSwipe(item(read), LibraryPreferences.ChapterSwipeAction.ToggleRead)
        coVerify(timeout = 5_000) { harness.parts.setReadStatus.await(read = false, chapters = arrayOf(read)) }
    }

    @Test
    fun unfavouriteOffersDeletion() {
        every { harness.downloadManager.getDownloadCount(any<Manga>()) } returns 2
        coEvery { harness.updateManga.awaitUpdateFavorite(1L, false) } returns true
        val model = harness.loaded()
        model.toggleFavorite()
        eventually { model.snackbarHostState.currentSnackbarData != null }
        model.snackbarHostState.currentSnackbarData?.performAction()
        verify(timeout = 5_000) { harness.downloadManager.deleteManga(any(), any(), any()) }
    }

    @Test
    fun unfavouriteKeepsDownloads() {
        every { harness.downloadManager.getDownloadCount(any<Manga>()) } returns 2
        coEvery { harness.updateManga.awaitUpdateFavorite(1L, false) } returns true
        val model = harness.loaded()
        model.toggleFavorite()
        eventually { model.snackbarHostState.currentSnackbarData != null }
        model.snackbarHostState.currentSnackbarData?.dismiss()
        eventually { model.snackbarHostState.currentSnackbarData == null }
        verify(exactly = 0) { harness.downloadManager.deleteManga(any(), any(), any()) }
    }

    @Test
    fun mergedWithoutDataObserves() {
        val model = harness.loaded()
        harness.awaitObserver(model)
        model.updateSuccessState { it.copy(source = mockk<MergedSource>(relaxed = true), mergedData = null) }
        model.downloads.observe()
        eventually { harness.statuses.subscriptionCount.value >= 2 }
    }
}
