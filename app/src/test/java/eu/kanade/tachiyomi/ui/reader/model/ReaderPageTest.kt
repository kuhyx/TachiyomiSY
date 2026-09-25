package eu.kanade.tachiyomi.ui.reader.model

import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.readerChapter
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream

internal class ReaderPageTest {

    @Test
    fun defaultsAreEmpty() {
        val page = ReaderPage(3)
        page.index shouldBe 3
        page.url shouldBe ""
        page.imageUrl.shouldBeNull()
        page.stream.shouldBeNull()
        page.shiftedPage shouldBe false
        page.isolatedPage shouldBe false
        page.fullPage shouldBe false
    }

    @Test
    fun fullPageClearsShift() {
        val page = ReaderPage(0, shiftedPage = true, isolatedPage = true)
        page.fullPage = false
        page.shiftedPage shouldBe true
        page.fullPage = true
        page.shiftedPage shouldBe false
        page.isolatedPage shouldBe true
    }

    @Test
    fun insertPageCopiesParent() {
        val chapter = readerChapter()
        val stream = { ByteArrayInputStream(byteArrayOf(1)) }
        val parent = ReaderPage(2, url = "/u", imageUrl = "/i", stream = stream)
        parent.chapter = chapter
        val insert = InsertPage(parent)
        insert.parent shouldBe parent
        insert.index shouldBe 2
        insert.url shouldBe "/u"
        insert.imageUrl shouldBe "/i"
        insert.chapter shouldBe chapter
        insert.stream shouldBe stream
        insert.status shouldBe Page.State.Ready
        val other = readerChapter(id = 9L)
        insert.chapter = other
        insert.chapter shouldBe other
    }

    @Test
    fun transitionEqualsBothWays() {
        val a = readerChapter(id = 1L)
        val b = readerChapter(id = 2L)
        val next = ChapterTransition.Next(a, b)
        (next == next) shouldBe true
        next shouldBe ChapterTransition.Next(a, b)
        next shouldBe ChapterTransition.Prev(b, a)
        next.equals("x") shouldBe false
        next shouldNotBe ChapterTransition.Next(a, readerChapter(id = 3L))
        next shouldNotBe ChapterTransition.Next(readerChapter(id = 3L), a)
    }

    @Test
    fun transitionHashAndString() {
        val a = readerChapter(id = 1L)
        val b = readerChapter(id = 2L)
        ChapterTransition.Next(a, b).hashCode() shouldBe ChapterTransition.Prev(a, b).hashCode()
        ChapterTransition.Next(a, null).hashCode() shouldBe 31 * a.hashCode()
        ChapterTransition.Prev(a, null).toString() shouldBe "Prev(from=/chapter/1, to=null)"
        ChapterTransition.Next(a, b).toString() shouldBe "Next(from=/chapter/1, to=/chapter/2)"
        ChapterTransition.Next(a, b).to shouldBe b
        ChapterTransition.Prev(a, b).from shouldBe a
    }
}
