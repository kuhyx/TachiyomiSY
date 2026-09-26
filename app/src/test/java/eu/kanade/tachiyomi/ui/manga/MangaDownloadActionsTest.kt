package eu.kanade.tachiyomi.ui.manga

import eu.kanade.presentation.manga.DownloadAction
import eu.kanade.presentation.manga.components.ChapterDownloadAction
import eu.kanade.tachiyomi.data.download.deleteChapters
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.download.removeFromDownloadQueue
import eu.kanade.tachiyomi.source.online.all.MergedSource
import exh.source.EH_SOURCE_ID
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga

@RunWith(RobolectricTestRunner::class)
internal class MangaDownloadActionsTest {
    private val harness = MangaHarness()
    private val manager get() = harness.downloadManager

    @Before
    fun setUp() {
        harness.start()
        harness.mangaFlow.value = manga(favorite = true) to listOf(
            chapter(1L, read = true),
            chapter(2L, bookmark = true),
            chapter(3L),
        )
        every { manager.downloader.isRunning } returns true
        mockkStatic(DELETION)
    }

    @After
    fun tearDown() {
        harness.stop()
        unmockkStatic(DELETION)
    }

    private fun downloaded(chapters: List<Chapter>) =
        verify(timeout = 5_000) { manager.downloadChapters(any(), chapters, any()) }

    @Test
    fun startQueuesAndRestartsErrors() {
        val downloads = harness.loaded().downloads
        downloads.runChapterDownloadActions(listOf(item(chapter(3L))), ChapterDownloadAction.START)
        downloaded(listOf(chapter(3L)))
        val failed = item(chapter(2L), state = Download.State.ERROR)
        downloads.runChapterDownloadActions(listOf(failed), ChapterDownloadAction.START)
        downloaded(listOf(chapter(2L)))
        verify { manager.downloader.isRunning }
    }

    @Test
    fun startNowNeedsASingleItem() {
        val downloads = harness.loaded().downloads
        val many = listOf(item(chapter(2L)), item(chapter(3L)))
        downloads.runChapterDownloadActions(many, ChapterDownloadAction.START_NOW)
        harness.queue.value = listOf(
            download(chapter(1L), Download.State.QUEUE),
            download(chapter(3L), Download.State.QUEUE),
        )
        downloads.runChapterDownloadActions(listOf(item(chapter(3L))), ChapterDownloadAction.START_NOW)
        eventually { harness.queue.value.first().chapter.id == 3L }
    }

    @Test
    fun cancelDropsTheQueuedDownload() {
        val model = harness.loaded()
        val queued = download(chapter(3L), Download.State.DOWNLOADING)
        val many = listOf(item(chapter(2L)), item(chapter(3L)))
        model.downloads.runChapterDownloadActions(many, ChapterDownloadAction.CANCEL)
        model.downloads.runChapterDownloadActions(listOf(item(chapter(3L))), ChapterDownloadAction.CANCEL)
        harness.queue.value = listOf(queued)
        model.downloads.runChapterDownloadActions(listOf(item(chapter(3L))), ChapterDownloadAction.CANCEL)
        queued.status shouldBe Download.State.NOT_DOWNLOADED
        verify { manager.removeFromDownloadQueue(listOf(chapter(3L))) }
    }

    @Test
    fun deleteRemovesChapters() {
        val model = harness.loaded()
        model.downloads.runChapterDownloadActions(listOf(item(chapter(3L))), ChapterDownloadAction.DELETE)
        verify(timeout = 5_000) { manager.deleteChapters(listOf(chapter(3L)), any(), any()) }
        harness.loading().downloads.deleteChapters(listOf(chapter(3L)))
    }

    @Test
    fun unreadAndBookmarkedActions() {
        val downloads = harness.loaded().downloads
        downloads.runDownloadAction(DownloadAction.UNREAD_CHAPTERS)
        downloaded(listOf(chapter(2L, bookmark = true), chapter(3L)))
        downloads.runDownloadAction(DownloadAction.BOOKMARKED_CHAPTERS)
        downloaded(listOf(chapter(2L, bookmark = true)))
    }

    @Test
    fun nextChaptersFollowTheSort() {
        val model = harness.loaded()
        model.downloads.runDownloadAction(DownloadAction.NEXT_1_CHAPTER)
        downloaded(listOf(chapter(2L, bookmark = true)))
        harness.mangaFlow.value = manga(flags = Manga.CHAPTER_SORT_ASC, favorite = true) to
            listOf(chapter(2L), chapter(3L))
        model.awaitSuccess { it.manga.chapterFlags == Manga.CHAPTER_SORT_ASC }
        model.downloads.runDownloadAction(DownloadAction.NEXT_5_CHAPTERS)
        downloaded(listOf(chapter(2L), chapter(3L)))
        harness.mangaFlow.value = manga(favorite = true) to listOf(chapter(1L, read = true))
        model.awaitSuccess { it.chapters.size == 1 }
        model.downloads.runDownloadAction(DownloadAction.NEXT_10_CHAPTERS)
        verify(exactly = 3) { manager.downloadChapters(any(), any(), any()) }
    }

    @Test
    fun skipFilteredUsesFiltered() {
        harness.readerPreferences.skipFiltered.set(true)
        harness.mangaFlow.value = manga(flags = Manga.CHAPTER_SHOW_BOOKMARKED, favorite = true) to
            listOf(chapter(2L, bookmark = true), chapter(3L))
        val model = harness.loaded()
        model.getUnreadChapters() shouldBe listOf(chapter(2L, bookmark = true))
        model.getBookmarkedChapters() shouldBe listOf(chapter(2L, bookmark = true))
        harness.loading().getUnreadChaptersSorted() shouldBe emptyList()
    }

    @Test
    fun mergedDownloadsGoPerMember() {
        val model = harness.loaded()
        val member = manga().copy(id = 1L, source = 8L)
        model.updateSuccessState {
            it.copy(source = mockk<MergedSource>(relaxed = true), mergedData = mergedData(member))
        }
        val stray = chapter(4L).copy(mangaId = 9L)
        model.downloads.downloadChapters(listOf(chapter(3L), stray))
        verify { manager.downloadChapters(member, listOf(chapter(3L)), any()) }
        verify(exactly = 0) { manager.downloadChapters(any(), listOf(stray), any()) }
        harness.loading().downloads.downloadChapters(listOf(chapter(3L)))
    }

    @Test
    fun newChaptersDownloadWhenAllowed() {
        val model = harness.loaded()
        coEvery { harness.parts.filterChaptersForDownload.await(any(), any()) } returnsMany
            listOf(emptyList(), listOf(chapter(3L)))
        model.downloads.downloadNewChapters(listOf(chapter(3L)))
        coVerify(timeout = 5_000) { harness.parts.filterChaptersForDownload.await(any(), any()) }
        model.downloads.downloadNewChapters(listOf(chapter(3L)))
        downloaded(listOf(chapter(3L)))
        harness.loading().downloads.downloadNewChapters(listOf(chapter(3L)))
    }

    @Test
    fun ehentaiNewChaptersStayQueued() {
        harness.mangaFlow.value = manga(source = EH_SOURCE_ID, favorite = true) to listOf(chapter(1L))
        coEvery { harness.parts.filterChaptersForDownload.await(any(), any()) } returns listOf(chapter(3L))
        val model = harness.loaded()
        model.downloads.downloadNewChapters(listOf(chapter(3L)))
        coVerify(timeout = 5_000) { harness.parts.filterChaptersForDownload.await(any(), any()) }
        verify(exactly = 0) { manager.downloadChapters(any(), any(), any()) }
    }
}
