package eu.kanade.tachiyomi.ui.reader

import eu.kanade.tachiyomi.data.download.cancelQueuedDownloads
import eu.kanade.tachiyomi.data.download.deletePendingChapters
import eu.kanade.tachiyomi.data.download.enqueueChaptersToDelete
import eu.kanade.tachiyomi.data.download.getQueuedDownloadOrNull
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.reader.loader.DownloadPageLoader
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private const val DELETION_KT = "eu.kanade.tachiyomi.data.download.DownloadManagerDeletionKt"

internal class ReaderChapterDownloadsTest {

    private val harness = ReaderVmHarness()

    @BeforeEach
    fun setUp() {
        harness.start()
        mockkStatic(DELETION_KT)
        every { harness.downloadManager.cancelQueuedDownloads(any()) } returns Unit
        every { harness.downloadManager.deletePendingChapters() } returns Unit
        coEvery { harness.downloadManager.enqueueChaptersToDelete(any(), any()) } returns Unit
        harness.chapters(domainChapter(1L, read = true), domainChapter(2L), domainChapter(3L))
    }

    @AfterEach
    fun tearDown() {
        harness.stop()
    }

    private fun nextDownloaded(value: Boolean) {
        every {
            harness.downloadManager.isChapterDownloaded(
                chapterName = "Chapter 3",
                chapterScanlator = any(),
                chapterUrl = any(),
                mangaTitle = any(),
                sourceId = any(),
                skipCache = any(),
            )
        } returns value
    }

    private fun readingSecond(): ReaderViewModel {
        harness.downloadPreferences.autoDownloadWhileReading.set(2)
        val vm = harness.loadedViewModel(chapterId = 2L)
        val current = vm.chapterList[1].also { it.pageLoader = mockk<DownloadPageLoader>() }
        vm.updateState { it.copy(viewerChapters = ViewerChapters(current, vm.chapterList[0], vm.chapterList[2])) }
        return vm
    }

    @Test
    fun downloadAheadNeedsEverything() {
        harness.loadedViewModel().chapterDownloads.downloadNextChapters()
        harness.downloadPreferences.autoDownloadWhileReading.set(2)
        harness.viewModel().chapterDownloads.downloadNextChapters()
        val vm = harness.loadedViewModel(chapterId = 2L)
        vm.chapterDownloads.downloadNextChapters()
        val current = vm.chapterList[1]
        vm.updateState { it.copy(viewerChapters = ViewerChapters(current, null, null)) }
        vm.chapterDownloads.downloadNextChapters()
        current.pageLoader = mockk<DownloadPageLoader>()
        vm.chapterDownloads.downloadNextChapters()
        coVerify(exactly = 0) { harness.getNextChapters.await(any(), any<Long>(), any()) }
    }

    @Test
    fun downloadsAheadWhenNextIsLocal() {
        nextDownloaded(true)
        coEvery { harness.getNextChapters.await(10L, 3L, any()) } returns
            listOf(domainChapter(4L), domainChapter(5L, number = 4.0), domainChapter(6L))
        readingSecond().chapterDownloads.downloadNextChapters()
        verify(timeout = 5_000) { harness.downloadManager.downloadChapters(harness.manga, match { it.size == 2 }) }
        harness.readerPreferences.skipDupe.set(true)
        readingSecond().chapterDownloads.downloadNextChapters()
        verify(timeout = 5_000) {
            harness.downloadManager.downloadChapters(harness.manga, match { it.map { c -> c.id } == listOf(4L, 6L) })
        }
    }

    @Test
    fun noDownloadWhenNextIsRemote() {
        nextDownloaded(false)
        readingSecond().chapterDownloads.downloadNextChapters()
        verify(timeout = 5_000) {
            harness.downloadManager.isChapterDownloaded(
                chapterName = "Chapter 3",
                chapterScanlator = any(),
                chapterUrl = any(),
                mangaTitle = any(),
                sourceId = any(),
                skipCache = any(),
            )
        }
        verify(exactly = 0) { harness.downloadManager.downloadChapters(any(), any()) }
    }

    @Test
    fun queuedDownloadIsCancelled() {
        val vm = harness.loadedViewModel()
        vm.chapterDownloads.cancelQueuedDownloads(readerChapter()).shouldBeNull()
        val download = mockk<Download>()
        every { harness.downloadManager.getQueuedDownloadOrNull(1L) } returns download
        vm.chapterDownloads.cancelQueuedDownloads(readerChapter()) shouldBe download
        verify { harness.downloadManager.cancelQueuedDownloads(listOf(download)) }
    }

    @Test
    fun deletesChaptersBehind() {
        val vm = harness.loadedViewModel(chapterId = 2L)
        val pending = mockk<Download>()
        vm.chapterToDownload = pending
        vm.chapterDownloads.deleteChapterIfNeeded(vm.chapterList[1])
        vm.chapterToDownload shouldBe pending
        harness.downloadPreferences.removeAfterReadSlots.set(1)
        vm.chapterDownloads.deleteChapterIfNeeded(vm.chapterList[0])
        vm.chapterToDownload.shouldBeNull()
        vm.chapterDownloads.deleteChapterIfNeeded(vm.chapterList[1])
        coVerify(timeout = 5_000) { harness.downloadManager.enqueueChaptersToDelete(any(), harness.manga) }
    }

    @Test
    fun deleteNeedsReadAndManga() {
        val vm = harness.loadedViewModel()
        vm.chapterDownloads.enqueueDeleteReadChapters(readerChapter(read = false))
        harness.viewModel().chapterDownloads.enqueueDeleteReadChapters(readerChapter(read = true))
        vm.updateState { it.copy(mergedManga = emptyMap()) }
        vm.chapterDownloads.enqueueDeleteReadChapters(readerChapter(read = true))
        coVerify(timeout = 5_000, exactly = 1) { harness.downloadManager.enqueueChaptersToDelete(any(), any()) }
    }

    @Test
    fun mergedDeleteUsesChildManga() {
        val child = harness.manga.copy(id = 10L, ogTitle = "Child")
        val vm = harness.loadedViewModel()
        vm.updateState { it.copy(mergedManga = mapOf(10L to child)) }
        vm.chapterDownloads.enqueueDeleteReadChapters(readerChapter(read = true))
        coVerify(timeout = 5_000) { harness.downloadManager.enqueueChaptersToDelete(any(), child) }
        vm.updateState { it.copy(mergedManga = mapOf(99L to child)) }
        vm.chapterDownloads.enqueueDeleteReadChapters(readerChapter(read = true))
        coVerify(exactly = 1) { harness.downloadManager.enqueueChaptersToDelete(any(), any()) }
    }

    @Test
    fun pendingDeletesRun() {
        harness.loadedViewModel().onActivityFinish()
        verify(timeout = 5_000) { harness.downloadManager.deletePendingChapters() }
        verify(timeout = 5_000) { harness.tempFileManager.deleteTempFiles() }
    }
}
