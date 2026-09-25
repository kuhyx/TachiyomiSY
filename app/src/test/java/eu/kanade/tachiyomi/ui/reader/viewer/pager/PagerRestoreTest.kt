package eu.kanade.tachiyomi.ui.reader.viewer.pager

import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderItem
import eu.kanade.tachiyomi.ui.reader.readerChapter
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class PagerRestoreTest {

    private val a = readerChapter(id = 1L)
    private val b = readerChapter(id = 2L)
    private val aPages = pagesOf(a, count = 3)
    private val bPages = pagesOf(b, count = 3)
    private val next = ChapterTransition.Next(a, b)

    private val joined: List<Pair<ReaderItem, ReaderItem?>> = listOf(
        aPages[0] to aPages[1],
        aPages[2] to null,
        next to null,
        bPages[0] to bPages[1],
        bPages[2] to null,
    )

    @Test
    fun joinedItemsFoundDirectly() {
        joined.joinedIndexOf(aPages[1]) shouldBe 0
        joined.joinedIndexOf(aPages[2]) shouldBe 1
        joined.joinedIndexOf(next) shouldBe 2
        joined.joinedIndexOf(null) shouldBe 1
    }

    @Test
    fun unjoinedTransitionFindsChapter() {
        val other = readerChapter(id = 9L)
        joined.joinedIndexOf(ChapterTransition.Next(other, a)) shouldBe 0
        joined.joinedIndexOf(ChapterTransition.Prev(other, a)) shouldBe 1
        // No page of the target chapter: the lookup falls to the first spread with a blank half.
        joined.joinedIndexOf(ChapterTransition.Next(a, other)) shouldBe 1
        joined.joinedIndexOf(ChapterTransition.Prev(a, other)) shouldBe 1
    }

    @Test
    fun restoresOldCurrent() {
        val old = aPages[0] to aPages[1]
        pageToRestore(old, a, aPages, useSecondPage = false) shouldBe aPages[0]
        pageToRestore(old, a, aPages, useSecondPage = true) shouldBe aPages[1]
        pageToRestore(aPages[2] to null, a, aPages, useSecondPage = true) shouldBe aPages[2]
        pageToRestore(null, a, aPages, useSecondPage = true).shouldBeNull()
        pageToRestore(null, a, aPages, useSecondPage = false).shouldBeNull()
        pageToRestore(next to null, a, aPages, useSecondPage = false) shouldBe next
    }

    @Test
    fun chapterChangeJumpsToItsStart() {
        pageToRestore(aPages[0] to aPages[1], b, aPages + bPages, useSecondPage = false) shouldBe bPages[0]
        pageToRestore(aPages[2] to ChapterTransition.Next(b, a), b, bPages, useSecondPage = false) shouldBe
            aPages[2]
        pageToRestore(aPages[2] to next, b, bPages, useSecondPage = false) shouldBe bPages[0]
        pageToRestore(aPages[0] to null, b, listOf(next) + bPages, useSecondPage = false) shouldBe bPages[0]
        pageToRestore(aPages[0] to null, b, aPages, useSecondPage = false).shouldBeNull()
    }

    @Test
    fun transitionAsSecondHalfIsJoined() {
        val mixed: List<Pair<ReaderItem, ReaderItem?>> = listOf(aPages[0] to next)
        mixed.joinedIndexOf(next) shouldBe 0
    }
}
