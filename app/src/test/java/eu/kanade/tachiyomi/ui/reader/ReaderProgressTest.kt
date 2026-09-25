package eu.kanade.tachiyomi.ui.reader

import eu.kanade.tachiyomi.data.sync.SyncDataJob
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import exh.source.EH_SOURCE_ID
import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.ChapterUpdate
import tachiyomi.domain.library.service.LibraryPreferences

internal class ReaderProgressTest {

    private val harness = ReaderVmHarness()

    @BeforeEach
    fun setUp() {
        harness.start()
        mockkObject(SyncDataJob.Companion)
        every { SyncDataJob.startNow(any(), any()) } returns Unit
        harness.chapters(domainChapter(1L), domainChapter(2L), domainChapter(3L, number = 1.0))
    }

    @AfterEach
    fun tearDown() {
        harness.stop()
    }

    private fun sync(onRead: Boolean, onOpen: Boolean) {
        harness.syncPreferences.syncService.set(1)
        harness.store.getBoolean("sync_on_chapter_read", false).set(onRead)
        harness.store.getBoolean("sync_on_chapter_open", false).set(onOpen)
    }

    private fun update(vm: ReaderViewModel, chapter: ReaderChapter, index: Int, extra: Boolean = false) {
        val page = chapter.pages?.getOrNull(index) ?: ReaderPage(index)
        runBlocking { vm.progress.updateChapterProgress(chapter, page, extra) }
    }

    @Test
    fun middlePageSavesPosition() {
        val vm = harness.loadedViewModel()
        val chapter = vm.chapterList[0]
        loadedPages(chapter, count = 4)
        update(vm, chapter, index = 1)
        vm.state.value.currentPage shouldBe 2
        vm.chapterPageIndex shouldBe 1
        chapter.chapter.lastPageRead shouldBe 1
        chapter.chapter.read shouldBe false
        coVerify { harness.updateChapter.await(ChapterUpdate(id = 1L, read = false, lastPageRead = 1L)) }
    }

    @Test
    fun lastPageMarksRead() {
        sync(onRead = true, onOpen = false)
        harness.trackPreferences.autoUpdateTrack.set(false)
        val vm = harness.loadedViewModel()
        val chapter = vm.chapterList[0]
        loadedPages(chapter, count = 3)
        update(vm, chapter, index = 2)
        chapter.chapter.read shouldBe true
        verify { SyncDataJob.startNow(harness.context, any()) }
    }

    @Test
    fun spreadEndMarksRead() {
        val vm = harness.loadedViewModel()
        val chapter = vm.chapterList[0]
        loadedPages(chapter, count = 3)
        update(vm, chapter, index = 1, extra = true)
        chapter.chapter.read shouldBe true
        coVerify(timeout = 5_000) { harness.trackChapter.await(harness.context, 10L, any(), any()) }
        val other = vm.chapterList[1]
        update(vm, other, index = 0, extra = true)
        other.chapter.read shouldBe false
    }

    @Test
    fun skippedWhenIncognitoOrFailed() {
        every { harness.getIncognitoState.await(any()) } returns true
        val vm = harness.loadedViewModel()
        val chapter = vm.chapterList[0]
        loadedPages(chapter, count = 1)
        update(vm, chapter, index = 0)
        chapter.chapter.read shouldBe false
        val failing = harness.loadedViewModel()
        every { harness.getIncognitoState.await(any()) } returns false
        val page = loadedPages(failing.chapterList[0], count = 1)[0]
        page.status = Page.State.Error(IllegalStateException("x"))
        runBlocking { failing.progress.updateChapterProgress(failing.chapterList[0], page, false) }
        failing.chapterList[0].chapter.read shouldBe false
    }

    @Test
    fun openingChapterSyncs() {
        sync(onRead = false, onOpen = true)
        val vm = harness.loadedViewModel()
        val chapter = vm.chapterList[0]
        loadedPages(chapter, count = 3)
        update(vm, chapter, index = 1)
        verify(exactly = 0) { SyncDataJob.startNow(any(), any()) }
        update(vm, chapter, index = 0)
        verify(exactly = 1) { SyncDataJob.startNow(any(), any()) }
        update(vm, chapter, index = 2)
        sync(onRead = false, onOpen = false)
        update(vm, chapter, index = 0)
        harness.syncPreferences.syncService.set(0)
        update(vm, chapter, index = 2)
        verify(exactly = 1) { SyncDataJob.startNow(any(), any()) }
    }

    @Test
    fun ehMarksLaterChaptersRead() {
        val vm = harness.viewModel()
        vm.updateState { it.copy(manga = harness.manga.copy(source = EH_SOURCE_ID)) }
        vm.chapterId = 2L
        val chapter = vm.chapterList.first { it.chapter.id == 2L }
        loadedPages(chapter, count = 1)
        update(vm, chapter, index = 0)
        coVerify(timeout = 5_000) { harness.updateChapter.awaitAll(listOf(ChapterUpdate(id = 1L, read = true))) }
    }

    @Test
    fun duplicatesMarkedRead() {
        harness.libraryPreferences.markDuplicateReadChapterAsRead.set(
            setOf(LibraryPreferences.MARK_DUPLICATE_CHAPTER_READ_EXISTING),
        )
        harness.downloadPreferences.removeAfterReadSlots.set(0)
        harness.chapters(
            domainChapter(1L),
            domainChapter(2L, number = 1.0, read = true),
            domainChapter(3L, number = 1.0),
            domainChapter(4L, number = -1.0),
        )
        val vm = harness.loadedViewModel()
        val chapter = vm.chapterList[0]
        chapter.chapter.chapter_number = 1f
        loadedPages(chapter, count = 1)
        update(vm, chapter, index = 0)
        coVerify {
            harness.updateChapter.awaitAll(listOf(ChapterUpdate(id = 1L, read = true), ChapterUpdate(id = 3L, read = true)))
        }
        chapter.chapter.chapter_number = -1f
        update(vm, chapter, index = 0)
        coVerify { harness.updateChapter.awaitAll(emptyList()) }
    }

    @Test
    fun noMangaSkipsTracking() {
        val vm = harness.viewModel()
        val chapter = readerChapter()
        loadedPages(chapter, count = 1)
        update(vm, chapter, index = 0)
        chapter.chapter.read shouldBe true
        coVerify(exactly = 0) { harness.trackChapter.await(any(), any(), any(), any()) }
    }

    @Test
    fun historyUsesReadTimer() {
        runBlocking {
            val vm = harness.loadedViewModel()
            vm.progress.updateHistory()
            coVerify(exactly = 0) { harness.upsertHistory.await(any()) }
            vm.updateState { it.copy(viewerChapters = ViewerChapters(vm.chapterList[0], null, null)) }
            vm.progress.updateHistory()
            coVerify { harness.upsertHistory.await(match { it.chapterId == 1L && it.sessionReadDuration == 0L }) }
            vm.progress.restartReadTimer()
            vm.progress.updateHistory()
            coVerify(exactly = 2) { harness.upsertHistory.await(any()) }
            every { harness.getIncognitoState.await(any()) } returns true
            val incognito = harness.loadedViewModel()
            incognito.updateState { it.copy(viewerChapters = ViewerChapters(vm.chapterList[0], null, null)) }
            incognito.progress.updateHistory()
            coVerify(exactly = 2) { harness.upsertHistory.await(any()) }
        }
    }
}
