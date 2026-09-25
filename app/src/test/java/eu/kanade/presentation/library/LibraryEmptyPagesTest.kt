package eu.kanade.presentation.library

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import eu.kanade.presentation.util.PresentationKoin
import io.kotest.matchers.collections.shouldContain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.model.LibraryDisplayMode

@RunWith(RobolectricTestRunner::class)
internal class LibraryEmptyPagesTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = PresentationKoin()
    private val harness = LibraryContentHarness(compose)
    private val default = libraryCategory(Category.UNCATEGORIZED_ID, name = "")

    @Before
    fun setUp() = koin.start()

    @After
    fun tearDown() = koin.stop()

    @Test
    fun emptyCategoryMessage() {
        harness.show(listOf(default), emptyMap())
        compose.onNodeWithText("Category is empty").assertExists()
        compose.onNodeWithText("Default").assertDoesNotExist()
    }

    @Test
    fun filtersExplainTheEmptyPage() {
        harness.show(listOf(default), emptyMap(), LibraryShow(hasActiveFilters = true))
        compose.onNodeWithText("No match found").assertExists()
    }

    @Test
    fun searchOffersGlobalSearch() {
        harness.show(listOf(default), emptyMap(), LibraryShow(searchQuery = "needle"))
        compose.onNodeWithText("No results found").assertExists()
        compose.onNodeWithText("globally", substring = true).performClick()
        harness.events shouldContain "global"
    }

    @Test
    fun emptySearchQueryIsNoSearch() {
        harness.show(listOf(default), emptyMap(), LibraryShow(searchQuery = ""))
        compose.onNodeWithText("Category is empty").assertExists()
    }

    @Test
    fun singleRealCategoryShowsTabs() {
        harness.show(listOf(libraryCategory(5L)), emptyMap())
        compose.onNodeWithText("Cat 5").assertExists()
    }

    @Test
    fun hiddenTabs() {
        harness.show(listOf(libraryCategory(5L), libraryCategory(6L)), emptyMap(), LibraryShow(showPageTabs = false))
        compose.onNodeWithText("Cat 5").assertDoesNotExist()
    }

    @Test
    fun gridSearchShowsGlobalItem() {
        val items = mapOf(5L to listOf(libraryItem(1L)))
        harness.show(listOf(libraryCategory(5L)), items, LibraryShow(searchQuery = "q"))
        compose.onNodeWithText("globally", substring = true).performClick()
        harness.events shouldContain "global"
    }

    @Test
    fun comfortableGridSearchAndAutoColumns() {
        val items = mapOf(5L to listOf(libraryItem(1L)))
        harness.show(
            listOf(libraryCategory(5L)),
            items,
            LibraryShow(displayMode = LibraryDisplayMode.ComfortableGrid, columns = 0, searchQuery = "q"),
        )
        compose.onNodeWithText("globally", substring = true).performClick()
        harness.events shouldContain "global"
    }

    @Test
    fun pageOutOfRangeIsClamped() {
        harness.show(listOf(libraryCategory(5L), libraryCategory(6L)), emptyMap(), LibraryShow(currentPage = 9))
        harness.events shouldContain "page 1"
    }
}
