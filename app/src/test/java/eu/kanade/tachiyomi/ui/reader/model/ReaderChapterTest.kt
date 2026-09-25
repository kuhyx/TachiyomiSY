package eu.kanade.tachiyomi.ui.reader.model

import eu.kanade.tachiyomi.ui.reader.loadedPages
import eu.kanade.tachiyomi.ui.reader.loader.PageLoader
import eu.kanade.tachiyomi.ui.reader.readerChapter
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter

internal class ReaderChapterTest {

    @Test
    fun pagesOnlyWhenLoaded() {
        val chapter = readerChapter()
        chapter.pages.shouldBeNull()
        val pages = loadedPages(chapter, count = 2)
        chapter.pages shouldBe pages
        chapter.state = ReaderChapter.State.Error(IllegalStateException("x"))
        chapter.pages.shouldBeNull()
    }

    @Test
    fun domainConstructorConverts() {
        val chapter = ReaderChapter(Chapter.create().copy(id = 4L, url = "/c/4", name = "Four"))
        chapter.chapter.id shouldBe 4L
        chapter.chapter.url shouldBe "/c/4"
        chapter.requestedPage shouldBe 0
        chapter.state shouldBe ReaderChapter.State.Wait
    }

    @Test
    fun lastUnrefRecyclesLoader() {
        val chapter = readerChapter()
        val loader = mockk<PageLoader>(relaxed = true)
        every { loader.recycle() } returns Unit
        chapter.pageLoader = loader
        chapter.state = ReaderChapter.State.Loading
        chapter.ref()
        chapter.ref()
        chapter.unref()
        chapter.pageLoader shouldBe loader
        chapter.state shouldBe ReaderChapter.State.Loading
        chapter.unref()
        verify(exactly = 1) { loader.recycle() }
        chapter.pageLoader.shouldBeNull()
        chapter.state shouldBe ReaderChapter.State.Wait
    }

    @Test
    fun unrefWithoutLoader() {
        val chapter = readerChapter()
        chapter.state = ReaderChapter.State.Loading
        chapter.ref()
        chapter.unref()
        chapter.state shouldBe ReaderChapter.State.Wait
    }

    @Test
    fun equalityByChapter() {
        val chapter = readerChapter(id = 1L)
        (chapter == chapter) shouldBe true
        chapter shouldBe readerChapter(id = 1L)
        chapter shouldNotBe readerChapter(id = 2L)
        chapter.equals("other") shouldBe false
        chapter.hashCode() shouldBe readerChapter(id = 1L).hashCode()
    }

    @Test
    fun stateValueClasses() {
        val error = IllegalStateException("boom")
        ReaderChapter.State.Error(error).error shouldBe error
        ReaderChapter.State.Loaded(emptyList()).pages shouldBe emptyList()
        ReaderChapter.State.Wait.toString() shouldBe "Wait"
        ReaderChapter.State.Loading.toString() shouldBe "Loading"
    }

    @Test
    fun viewerChaptersRefAll() {
        val curr = readerChapter(id = 1L)
        val prev = readerChapter(id = 2L)
        val next = readerChapter(id = 3L)
        listOf(curr, prev, next).forEach { it.state = ReaderChapter.State.Loading }
        val chapters = ViewerChapters(curr, prev, next)
        chapters.ref()
        chapters.unref()
        listOf(curr, prev, next).forEach { it.state shouldBe ReaderChapter.State.Wait }
    }

    @Test
    fun viewerChaptersWithoutPeers() {
        val curr = readerChapter(id = 1L)
        curr.state = ReaderChapter.State.Loading
        val chapters = ViewerChapters(curr, prevChapter = null, nextChapter = null)
        chapters.ref()
        chapters.unref()
        curr.state shouldBe ReaderChapter.State.Wait
        chapters.copy(prevChapter = curr).prevChapter shouldBe curr
    }
}
