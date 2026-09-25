package eu.kanade.tachiyomi.ui.reader

import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.ChapterUpdate

internal class ReaderBookmarksTest {

    private val harness = ReaderVmHarness()
    private val source = mockk<HttpSource>()

    @BeforeEach
    fun setUp() {
        harness.start()
        every { harness.sourceManager.getOrStub(1L) } returns source
    }

    @AfterEach
    fun tearDown() {
        harness.stop()
    }

    private fun withChapter(vm: ReaderViewModel) {
        vm.updateState { it.copy(viewerChapters = ViewerChapters(readerChapter(id = 1L), null, null)) }
    }

    @Test
    fun sourceNeedsHttpManga() {
        harness.viewModel().getSource().shouldBeNull()
        val vm = harness.loadedViewModel()
        vm.getSource() shouldBe source
        every { harness.sourceManager.getOrStub(1L) } returns mockk<Source>()
        vm.getSource().shouldBeNull()
    }

    @Test
    fun chapterUrlFromSource() {
        val vm = harness.loadedViewModel()
        vm.getChapterUrl().shouldBeNull()
        withChapter(vm)
        every { source.getChapterUrl(any()) } returns "https://c/1"
        vm.getChapterUrl() shouldBe "https://c/1"
        every { source.getChapterUrl(any()) } throws IllegalStateException("bad")
        vm.getChapterUrl().shouldBeNull()
        every { harness.sourceManager.getOrStub(1L) } returns mockk<Source>()
        vm.getChapterUrl().shouldBeNull()
    }

    @Test
    fun currentBookmarkToggles() {
        val vm = harness.loadedViewModel()
        vm.toggleChapterBookmark()
        vm.state.value.bookmarked shouldBe false
        withChapter(vm)
        vm.toggleChapterBookmark()
        vm.state.value.bookmarked shouldBe true
        vm.state.value.currentChapter!!.chapter.bookmark shouldBe true
        coVerify(timeout = 5_000) { harness.updateChapter.await(ChapterUpdate(id = 1L, bookmark = true)) }
        vm.toggleChapterBookmark()
        vm.state.value.bookmarked shouldBe false
    }

    @Test
    fun listBookmarkToggles() {
        harness.chapters(domainChapter(1L), domainChapter(2L))
        val vm = harness.loadedViewModel()
        vm.toggleBookmark(chapterId = 9L, bookmarked = true)
        vm.toggleBookmark(chapterId = 2L, bookmarked = true)
        vm.chapterList.first { it.chapter.id == 2L }.chapter.bookmark shouldBe true
        coVerify(timeout = 5_000) { harness.updateChapter.await(ChapterUpdate(id = 2L, bookmark = true)) }
        coVerify(exactly = 0) { harness.updateChapter.await(ChapterUpdate(id = 9L, bookmark = true)) }
    }
}
