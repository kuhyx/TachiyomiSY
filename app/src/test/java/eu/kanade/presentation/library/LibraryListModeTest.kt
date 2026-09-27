package eu.kanade.presentation.library

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import eu.kanade.presentation.util.PresentationKoin
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import tachiyomi.domain.library.model.LibraryDisplayMode

/** The list display mode, and a pager asked for a page past its last category. */
@RunWith(RobolectricTestRunner::class)
internal class LibraryListModeTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = PresentationKoin()
    private val harness = LibraryContentHarness(compose)
    private val categories = listOf(libraryCategory(1L), libraryCategory(2L))
    private val items by lazy {
        mapOf(
            1L to listOf(libraryItem(10L, unread = 3L)),
            2L to listOf(libraryItem(20L)),
        )
    }

    @Before
    fun setUp() = koin.start()

    @After
    fun tearDown() = koin.stop()

    @Test
    fun listRowsWithoutContinueReading() {
        harness.show(categories, items, LibraryShow(displayMode = LibraryDisplayMode.List, continueReading = false))
        compose.onNodeWithText("Manga 10").assertExists()
        compose.onAllNodesWithContentDescription("Resume").fetchSemanticsNodes().size shouldBe 0
    }

    @Test
    fun readRowsHaveNoResume() {
        harness.show(categories, items, LibraryShow(displayMode = LibraryDisplayMode.List, currentPage = 1))
        compose.onNodeWithText("Manga 20").assertExists()
        compose.onAllNodesWithContentDescription("Resume").fetchSemanticsNodes().size shouldBe 0
    }

    @Test
    fun aPagePastTheEndSnapsBack() {
        harness.show(categories, items, LibraryShow(currentPage = 5))
        compose.waitForIdle()
        compose.onNodeWithText("Manga 20").assertExists()
    }

    @Test
    fun aVanishingPageSnapsBack() {
        harness.show(categories, items, LibraryShow(currentPage = 1))
        compose.onNodeWithText("Manga 20").assertExists()
        harness.shownCategories = listOf(libraryCategory(1L))
        compose.waitForIdle()
        compose.onNodeWithText("Manga 10").assertExists()
    }

    @Test
    fun anEmptyQueryHasNoGlobalSearch() {
        harness.show(categories, items, LibraryShow(displayMode = LibraryDisplayMode.List, searchQuery = ""))
        compose.onNodeWithText("Manga 10").assertExists()
    }

    @Test
    @Config(qualifiers = "land")
    fun landscapeUsesItsColumns() {
        harness.show(categories, items, LibraryShow(displayMode = LibraryDisplayMode.CompactGrid))
        compose.onNodeWithText("Manga 10").assertExists()
    }
}
