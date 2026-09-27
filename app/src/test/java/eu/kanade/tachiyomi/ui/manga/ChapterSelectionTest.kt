package eu.kanade.tachiyomi.ui.manga

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

internal class ChapterSelectionTest {
    private val selection = ChapterSelection()

    private fun List<ChapterList.Item>.selectedIds() = filter { it.selected }.map { it.id }

    @Test
    fun toggleSelectsOneRow() {
        val chapters = items(3)
        val (result, after) = selection.toggle(chapters, chapters[1], selected = true, fromLongPress = false)
        result.selectedIds() shouldContainExactly listOf(2L)
        after.selectedChapterIds shouldBe setOf(2L)
    }

    @Test
    fun longPressExtendsTheRange() {
        val start = items(5)
        val (first, anchored) = selection.toggle(start, start[0], selected = true, fromLongPress = true)
        val (range, _) = anchored.toggle(first, first[3], selected = true, fromLongPress = true)
        range.selectedIds() shouldContainExactly listOf(1L, 2L, 3L, 4L)
    }

    @Test
    fun anchorsFollowTheChapters() {
        // A chapter arrives above the anchor between two long presses; the range still starts at chapter 2.
        val start = items(4)
        val (first, anchored) = selection.toggle(start, start[1], selected = true, fromLongPress = true)
        val grown = listOf(item(chapter(9L))) + first
        val (range, _) = anchored.toggle(grown, grown[4], selected = true, fromLongPress = true)
        range.selectedIds() shouldContainExactly listOf(2L, 3L, 4L)
    }

    @Test
    fun retriedToggleIsIdempotent() {
        // MutableStateFlow.update re-runs its lambda under contention; the same input must give the same answer.
        val start = items(5)
        val (first, anchored) = selection.toggle(start, start[0], selected = true, fromLongPress = true)
        val once = anchored.toggle(first, first[3], selected = true, fromLongPress = true)
        val again = anchored.toggle(first, first[3], selected = true, fromLongPress = true)
        again shouldBe once
        anchored.selectedChapterIds shouldBe setOf(1L)
    }

    @Test
    fun toggleIgnoresUnknownRows() {
        val chapters = items(2)
        val stranger = item(chapter(9L))
        selection.toggle(chapters, stranger, selected = true, fromLongPress = false) shouldBe (chapters to selection)
    }

    @Test
    fun setAllSelectsAndClears() {
        val (all, selected) = selection.setAll(items(3), selected = true)
        all.selectedIds() shouldContainExactly listOf(1L, 2L, 3L)
        selected.selectedChapterIds shouldBe setOf(1L, 2L, 3L)
        val (none, cleared) = selected.setAll(all, selected = false)
        none.selectedIds() shouldBe emptyList()
        cleared.selectedChapterIds shouldBe emptySet()
    }

    @Test
    fun invertFlipsEveryRow() {
        val chapters = listOf(item(chapter(1L), selected = true), item(chapter(2L)))
        val (inverted, after) = ChapterSelection(setOf(1L)).invert(chapters)
        inverted.selectedIds() shouldContainExactly listOf(2L)
        after.selectedChapterIds shouldBe setOf(2L)
    }

    @Test
    fun reapplyFollowsTheIds() {
        val chapters = listOf(item(chapter(1L), selected = true), item(chapter(2L)))
        ChapterSelection(setOf(2L)).reapply(chapters).selectedIds() shouldContainExactly listOf(2L)
    }
}
