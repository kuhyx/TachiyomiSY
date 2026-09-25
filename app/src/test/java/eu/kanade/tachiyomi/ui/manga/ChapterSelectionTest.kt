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
        val result = selection.toggle(chapters, chapters[1], selected = true, fromLongPress = false)
        result.selectedIds() shouldContainExactly listOf(2L)
        selection.selectedChapterIds shouldBe hashSetOf(2L)
    }

    @Test
    fun longPressExtendsTheRange() {
        var chapters = items(5)
        chapters = selection.toggle(chapters, chapters[0], selected = true, fromLongPress = true)
        chapters = selection.toggle(chapters, chapters[3], selected = true, fromLongPress = true)
        chapters.selectedIds() shouldContainExactly listOf(1L, 2L, 3L, 4L)
    }

    @Test
    fun toggleIgnoresUnknownRows() {
        val chapters = items(2)
        val stranger = item(chapter(9L))
        selection.toggle(chapters, stranger, selected = true, fromLongPress = false) shouldBe chapters
    }

    @Test
    fun setAllSelectsAndClears() {
        val all = selection.setAll(items(3), selected = true)
        all.selectedIds() shouldContainExactly listOf(1L, 2L, 3L)
        selection.selectedChapterIds shouldBe hashSetOf(1L, 2L, 3L)
        selection.setAll(all, selected = false).selectedIds() shouldBe emptyList()
        selection.selectedChapterIds shouldBe hashSetOf()
    }

    @Test
    fun invertFlipsEveryRow() {
        val chapters = listOf(item(chapter(1L), selected = true), item(chapter(2L)))
        val inverted = selection.invert(chapters)
        inverted.selectedIds() shouldContainExactly listOf(2L)
        selection.selectedChapterIds shouldBe hashSetOf(2L)
    }
}
