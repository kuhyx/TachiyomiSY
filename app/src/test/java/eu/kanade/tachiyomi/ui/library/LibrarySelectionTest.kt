package eu.kanade.tachiyomi.ui.library

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.Test

internal class LibrarySelectionTest {
    private val category = libCategory(1L)
    private val other = libCategory(2L)
    private val items = (1L..5L).map { libItem(it) }
    private val selection = LibrarySelection()

    private fun LibraryItem.entry() = libraryManga

    @Test
    fun toggleAddsAndRemoves() {
        val state = stateOf(category, items)
        val one = selection.toggle(state, category, items[0].entry())
        one.selection shouldContainExactly setOf(1L)
        selection.toggle(one, category, items[0].entry()).selection.shouldBeEmpty()
        selection.clear(one).selection.shouldBeEmpty()
    }

    @Test
    fun rangeFromOtherCategoryPicksOne() {
        val state = stateOf(category, items)
        val first = selection.toggle(state, other, items[0].entry())
        selection.toggleRange(first, category, items[3].entry()).selection shouldContainExactly setOf(1L, 4L)
    }

    @Test
    fun rangeForward() {
        val state = stateOf(category, items)
        val first = selection.toggle(state, category, items[1].entry())
        selection.toggleRange(first, category, items[3].entry()).selection shouldContainExactly setOf(2L, 3L, 4L)
    }

    @Test
    fun rangeBackward() {
        val state = stateOf(category, items)
        val first = selection.toggle(state, category, items[3].entry())
        selection.toggleRange(first, category, items[1].entry()).selection shouldContainExactly setOf(4L, 2L, 3L)
    }

    @Test
    fun rangeOnTheSameEntryAddsNothing() {
        val state = stateOf(category, items)
        val first = selection.toggle(state, category, items[2].entry())
        selection.toggleRange(first, category, items[2].entry()).selection shouldContainExactly setOf(3L)
    }

    @Test
    fun selectAllAndInvert() {
        val state = stateOf(category, items, selection = setOf(1L))
        selection.selectAll(state).selection shouldContainExactly setOf(1L, 2L, 3L, 4L, 5L)
        selection.invert(state).selection shouldContainExactly setOf(2L, 3L, 4L, 5L)
    }

    @Test
    fun noCategoryNothingToSelect() {
        val state = LibraryScreenModel.State(selection = setOf(9L))
        selection.selectAll(state).selection shouldContainExactly setOf(9L)
        selection.invert(state).selection shouldContainExactly setOf(9L)
    }
}
