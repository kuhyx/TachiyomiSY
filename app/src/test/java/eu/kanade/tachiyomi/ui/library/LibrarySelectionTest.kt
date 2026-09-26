package eu.kanade.tachiyomi.ui.library

import eu.kanade.tachiyomi.ui.base.customInfoModule
import eu.kanade.tachiyomi.ui.library.LibraryScreenModel.LibraryData
import eu.kanade.tachiyomi.ui.library.LibraryScreenModel.State
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import tachiyomi.domain.category.model.Category

internal class LibrarySelectionTest {
    private val first = Category(id = 1, name = "First", order = 0, flags = 0)
    private val second = Category(id = 2, name = "Second", order = 1, flags = 0)
    private val items by lazy { (1L..6L).map { libraryItem(manga(it)) } }
    private val state by lazy {
        State(
            libraryData = LibraryData(favorites = items),
            groupedFavorites = mapOf(first to listOf(1L, 2L, 3L, 4L), second to listOf(5L, 6L)),
        )
    }
    private val selection = LibrarySelection()

    @BeforeEach
    fun setUp() {
        startKoin { modules(customInfoModule()) }
    }

    @AfterEach
    fun tearDown() = stopKoin()

    private fun lm(id: Long) = items[id.toInt() - 1].libraryManga

    @Test
    fun toggleAddsAndRemoves() {
        val once = selection.toggle(state, first, lm(1))
        once.selection shouldBe setOf(1L)
        selection.toggle(once, first, lm(1)).selection shouldBe emptySet()
        // With the selection emptied, a range press starts over with just the pressed entry.
        selection.toggleRange(once.copy(selection = setOf()), first, lm(4)).selection shouldBe setOf(4L)
    }

    @Test
    fun rangeSelectsForward() {
        val start = selection.toggle(state, first, lm(1))
        selection.toggleRange(start, first, lm(3)).selection shouldBe setOf(1L, 2L, 3L)
    }

    @Test
    fun rangeSelectsBackward() {
        val start = selection.toggle(state, first, lm(4))
        selection.toggleRange(start, first, lm(2)).selection shouldBe setOf(2L, 3L, 4L)
    }

    @Test
    fun rangeOnTheSameEntry() {
        val start = selection.toggle(state, first, lm(2))
        selection.toggleRange(start, first, lm(2)).selection shouldBe setOf(2L)
    }

    @Test
    fun rangeInAnotherCategory() {
        val start = selection.toggle(state, first, lm(1))
        selection.toggleRange(start, second, lm(6)).selection shouldBe setOf(1L, 6L)
    }

    @Test
    fun rangeFromAForeignLastEntry() {
        // Pins the crash reported in the issue linked from the PR: the last selected entry is in another category.
        val a = selection.toggle(state, first, lm(1))
        val b = selection.toggle(a, second, lm(5))
        val back = selection.toggle(b, second, lm(5))
        shouldThrow<IndexOutOfBoundsException> { selection.toggleRange(back, second, lm(6)) }
    }

    @Test
    fun selectAllAndInvert() {
        val all = selection.selectAll(state.copy(selection = setOf(5L)))
        all.selection shouldBe setOf(5L, 1L, 2L, 3L, 4L)
        selection.invert(state.copy(selection = setOf(1L, 5L))).selection shouldBe setOf(5L, 2L, 3L, 4L)
        selection.clear(all).selection shouldBe emptySet()
        // Every bulk operation forgets the last press, so the next range press selects one entry.
        selection.toggleRange(state, first, lm(3)).selection shouldBe setOf(3L)
    }

    @Test
    fun noActiveCategory() {
        selection.selectAll(State(selection = setOf(9L))).selection shouldBe setOf(9L)
        selection.invert(State(selection = setOf(9L))).selection shouldBe setOf(9L)
    }
}
