package eu.kanade.tachiyomi.ui.reader.viewer.pager

import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderItem
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.readerChapter
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/** Renders a joined list as "0+1 2+_ T" (page indices, `_` a blank, `P`/`N` a transition). */
internal fun List<Pair<ReaderItem, ReaderItem?>>.render(): String = joinToString(" ") { (first, second) ->
    fun label(item: ReaderItem?): String = when (item) {
        null -> "_"
        is ChapterTransition.Prev -> "P"
        is ChapterTransition.Next -> "N"
        is ReaderPage -> "${item.chapter.chapter.id}.${item.index}"
    }
    if (second == null && first !is ReaderPage) label(first) else "${label(first)}+${label(second)}"
}

internal fun pagesOf(chapter: ReaderChapter, count: Int, full: Set<Int> = emptySet()): List<ReaderPage> =
    List(count) { index ->
        ReaderPage(index).also {
            it.chapter = chapter
            it.fullPage = index in full
        }
    }

internal class PagerDoublePagesTest {

    private val a = readerChapter(id = 1L)
    private val b = readerChapter(id = 2L)

    private fun join(items: List<ReaderItem>, shift: ReaderPage? = null, shifting: Boolean = false) =
        joinDoublePages(items, shift, shifting).render()

    @Test
    fun plainPagesPairUp() {
        join(pagesOf(a, count = 5)) shouldBe "1.0+1.1 1.2+1.3 1.4+_"
        join(emptyList()) shouldBe ""
    }

    @Test
    fun fullPageIsolatesEvenNeighbour() {
        val pages = pagesOf(a, count = 4, full = setOf(1))
        join(pages) shouldBe "1.0+_ 1.1+_ 1.2+1.3"
        pages[0].isolatedPage shouldBe true
    }

    @Test
    fun fullPagesWithoutIsolation() {
        join(pagesOf(a, count = 3, full = setOf(0))) shouldBe "1.0+_ 1.1+1.2"
        join(pagesOf(a, count = 4, full = setOf(2))) shouldBe "1.0+1.1 1.2+_ 1.3+_"
        join(pagesOf(a, count = 2, full = setOf(0, 1))) shouldBe "1.0+_ 1.1+_"
    }

    @Test
    fun shiftStartsAtFirstPage() {
        val pages = pagesOf(a, count = 4)
        join(pages, shift = pages[2], shifting = true) shouldBe "1.0+_ 1.1+1.2 1.3+_"
        pages[0].shiftedPage shouldBe true
        join(pages, shift = ReaderPage(9), shifting = true) shouldBe "1.0+_ 1.1+1.2 1.3+_"
        join(pages, shift = pages[2], shifting = false) shouldBe "1.0+1.1 1.2+1.3"
        pages[0].shiftedPage shouldBe false
    }

    @Test
    fun shiftSkipsPastFullPage() {
        val pages = pagesOf(a, count = 4, full = setOf(1))
        join(pages, shift = pages[3], shifting = true) shouldBe "1.0+_ 1.1+_ 1.2+_ 1.3+_"
        pages[2].shiftedPage shouldBe true
        val allFull = pagesOf(a, count = 2, full = setOf(0, 1))
        join(allFull, shift = allFull[1], shifting = true) shouldBe "1.0+_ 1.1+_"
    }

    @Test
    fun chaptersSplitRuns() {
        val items = pagesOf(a, count = 3) + ChapterTransition.Next(a, b) + pagesOf(b, count = 1)
        join(items) shouldBe "1.0+1.1 1.2+_ N 2.0+_"
        join(pagesOf(a, count = 1) + pagesOf(b, count = 1)) shouldBe "1.0+_ 2.0+_"
    }

    @Test
    fun transitionsAroundChapter() {
        val items = listOf(ChapterTransition.Prev(a, null)) + pagesOf(a, count = 2) +
            ChapterTransition.Next(a, b) + pagesOf(b, count = 2)
        join(items) shouldBe "P 1.0+1.1 N 2.0+2.1"
    }
}
