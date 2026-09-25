package eu.kanade.tachiyomi.ui.reader

import eu.kanade.tachiyomi.ui.reader.ReaderViewModel.Event
import eu.kanade.tachiyomi.ui.reader.loader.ChapterLoader
import eu.kanade.tachiyomi.ui.reader.model.InsertPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.viewer.Viewer
import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ReaderPageSelectionTest {

    private val harness = ReaderVmHarness()

    @BeforeEach
    fun setUp() {
        harness.start()
        harness.chapters(domainChapter(1L), domainChapter(2L))
    }

    @AfterEach
    fun tearDown() {
        harness.stop()
    }

    private fun current(vm: ReaderViewModel, index: Int = 0) {
        vm.updateState { it.copy(viewerChapters = ViewerChapters(vm.chapterList[index], null, null)) }
    }

    @Test
    fun chapterItemsMarkCurrent() {
        val vm = harness.loadedViewModel()
        vm.getChapters().map { it.isCurrent } shouldBe listOf(false, false)
        current(vm, index = 1)
        val items = vm.getChapters()
        items.map { it.isCurrent } shouldBe listOf(false, true)
        items[1].manga shouldBe harness.manga
        items[0].chapter.id shouldBe 1L
    }

    @Test
    fun viewerIsPublished() {
        val vm = harness.viewModel()
        val viewer = mockk<Viewer>()
        vm.onViewerLoaded(viewer)
        vm.state.value.viewer shouldBe viewer
        vm.onViewerLoaded(null)
        vm.state.value.viewer shouldBe null
    }

    @Test
    fun insertPagesAreIgnored() {
        val vm = harness.loadedViewModel()
        val page = ReaderPage(0).also { it.chapter = vm.chapterList[0] }
        vm.onPageSelected(InsertPage(page), currentPageText = "1", hasExtraPage = false)
        vm.state.value.currentPageText shouldBe ""
        vm.onPageSelected(page, currentPageText = "1", hasExtraPage = false)
        vm.state.value.currentPageText shouldBe "1"
    }

    @Test
    fun earlyPageOnCurrentChapter() {
        val vm = harness.loadedViewModel()
        current(vm)
        mockkObject(vm.chapterDownloads)
        val pages = loadedPages(vm.chapterList[0], count = 8)
        runBlocking {
            val event = async(start = CoroutineStart.UNDISPATCHED) { vm.eventFlow.first() }
            vm.onPageSelected(pages[1], currentPageText = "2", hasExtraPage = false)
            event.await() shouldBe Event.PageChanged
        }
        verify(exactly = 0) { vm.chapterDownloads.downloadNextChapters() }
        coVerify(timeout = 5_000) { harness.updateChapter.await(any()) }
    }

    @Test
    fun laterPageDownloadsAhead() {
        val vm = harness.loadedViewModel()
        current(vm)
        mockkObject(vm.chapterDownloads)
        val pages = loadedPages(vm.chapterList[0], count = 4)
        vm.onPageSelected(pages[2], currentPageText = "3", hasExtraPage = true)
        verify { vm.chapterDownloads.downloadNextChapters() }
    }

    @Test
    fun otherChapterBecomesActive() {
        val vm = harness.loadedViewModel()
        current(vm)
        vm.loader = mockk<ChapterLoader>(relaxed = true)
        val pages = loadedPages(vm.chapterList[1], count = 2)
        vm.onPageSelected(pages[0], currentPageText = "1", hasExtraPage = false)
        awaitUntil { vm.state.value.currentChapter == vm.chapterList[1] }
    }
}
