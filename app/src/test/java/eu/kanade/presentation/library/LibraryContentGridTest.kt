package eu.kanade.presentation.library

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import eu.kanade.presentation.util.PresentationKoin
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.library.model.LibraryDisplayMode

/** A grid with an empty search query offers no global search. */
@RunWith(RobolectricTestRunner::class)
internal class LibraryContentGridTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = PresentationKoin()
    private val harness = LibraryContentHarness(compose)
    private val categories = listOf(libraryCategory(1L))
    private val items by lazy { mapOf(1L to listOf(libraryItem(10L))) }

    private fun gridWithQuery(query: String) =
        LibraryShow(displayMode = LibraryDisplayMode.ComfortableGrid, searchQuery = query)

    @Before
    fun setUp() = koin.start()

    @After
    fun tearDown() = koin.stop()

    @Test
    fun emptyQueryNoGlobalSearch() {
        harness.show(categories, items, gridWithQuery(""))
        compose.onNodeWithText("globally", substring = true).assertDoesNotExist()
    }
}
