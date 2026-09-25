package eu.kanade.presentation.library

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import eu.kanade.presentation.util.PresentationKoin
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.library.model.LibraryDisplayMode

@RunWith(RobolectricTestRunner::class)
internal class LibraryContentTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = PresentationKoin()
    private val harness = LibraryContentHarness(compose)
    private val categories = listOf(libraryCategory(1L), libraryCategory(2L))
    private val items by lazy {
        mapOf(
            1L to listOf(libraryItem(10L, unread = 3L, downloads = 2, badgeUnread = 3L, language = "en")),
            2L to listOf(libraryItem(20L, local = true)),
        )
    }

    @Before
    fun setUp() = koin.start()

    @After
    fun tearDown() = koin.stop()

    @Test
    fun compactGridOpensAndSelects() {
        harness.show(categories, items)
        compose.onNodeWithText("EN").assertExists()
        compose.onNodeWithText("Manga 10").performClick()
        compose.onNodeWithText("Manga 10").performTouchInput { longClick() }
        compose.onAllNodesWithContentDescription("Resume")[0].performClick()
        harness.events shouldContainExactly listOf("page 0", "open 10", "range 1 10", "continue 10")
    }

    @Test
    fun selectionToggles() {
        harness.show(categories, items, LibraryShow(selection = setOf(10L)))
        compose.onNodeWithText("Manga 10").performClick()
        harness.events shouldContain "toggle 1 10"
    }

    @Test
    fun tabsSwitchCategories() {
        harness.show(categories, items)
        compose.onNodeWithText("Cat 2").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Manga 20").assertExists()
        harness.events shouldContain "page 1"
    }

    @Test
    fun coverOnlyGridHidesTitles() {
        harness.show(categories, items, LibraryShow(displayMode = LibraryDisplayMode.CoverOnlyGrid, columns = 0))
        compose.onNodeWithText("Manga 10").assertDoesNotExist()
        compose.onAllNodesWithContentDescription("Resume")[0].performClick()
        harness.events shouldContain "continue 10"
    }

    @Test
    fun comfortableGridShowsTitles() {
        harness.show(categories, items, LibraryShow(displayMode = LibraryDisplayMode.ComfortableGrid))
        compose.onNodeWithText("Manga 10").performClick()
        compose.onNodeWithText("Manga 10").performTouchInput { longClick() }
        compose.onAllNodesWithContentDescription("Resume")[0].performClick()
        harness.events shouldContainExactly listOf("page 0", "open 10", "range 1 10", "continue 10")
    }

    @Test
    fun listModeShowsRows() {
        harness.show(categories, items, LibraryShow(displayMode = LibraryDisplayMode.List, searchQuery = "q"))
        compose.onNodeWithText("Manga 10").performClick()
        compose.onNodeWithText("Manga 10").performTouchInput { longClick() }
        compose.onAllNodesWithContentDescription("Resume")[0].performClick()
        compose.onNodeWithText("globally", substring = true).performClick()
        harness.events shouldContainExactly listOf("page 0", "open 10", "range 1 10", "continue 10", "global")
    }

    @Test
    fun withoutContinueReadingNoButton() {
        harness.show(categories, items, LibraryShow(continueReading = false))
        compose.onAllNodesWithContentDescription("Resume").fetchSemanticsNodes().size shouldBe 0
    }

    @Test
    fun pullToRefresh() {
        harness.show(categories, items)
        compose.onNodeWithText("Manga 10").performTouchInput { swipeDown() }
        compose.mainClock.advanceTimeBy(2_000L)
        compose.waitForIdle()
        harness.events shouldContain "refresh"
    }

    @Test
    fun refusedRefreshStaysIdle() {
        harness.refreshStarts = false
        harness.show(categories, items)
        compose.onNodeWithText("Manga 10").performTouchInput { swipeDown() }
        compose.waitForIdle()
        harness.events shouldContain "refresh"
    }
}
