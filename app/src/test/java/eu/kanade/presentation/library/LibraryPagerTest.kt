package eu.kanade.presentation.library

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import eu.kanade.presentation.util.PresentationKoin
import io.kotest.matchers.collections.shouldContain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** A pager over many categories: pages farther than one from the current one are left empty. */
@RunWith(RobolectricTestRunner::class)
internal class LibraryPagerTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = PresentationKoin()
    private val harness = LibraryContentHarness(compose)
    private val categories = (1L..6L).map { libraryCategory(it) }
    private val items by lazy { categories.associate { it.id to listOf(libraryItem(it.id * 10)) } }

    @Before
    fun setUp() = koin.start()

    @After
    fun tearDown() = koin.stop()

    @Test
    fun swipingBothWays() {
        harness.show(categories, items, LibraryShow(currentPage = 2))
        compose.onNodeWithText("Manga 30").performTouchInput { swipeLeft() }
        compose.waitForIdle()
        compose.onNodeWithText("Manga 40").performTouchInput { swipeRight() }
        compose.waitForIdle()
        compose.onNodeWithText("Manga 30").assertExists()
        harness.events shouldContain "page 3"
    }

    @Test
    fun aFarTabScrollsAcross() {
        harness.show(categories, items)
        // The last tab sits past the screen edge in the scrollable tab row.
        compose.onNodeWithText("Cat 6").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Manga 60").assertExists()
        harness.events shouldContain "page 5"
    }

    @Test
    fun lostCategoryClampsThePager() {
        // The pager clamps itself when its page count drops under the current page; no snap-back is needed.
        harness.show(categories, items, LibraryShow(currentPage = 5))
        harness.shownCategories = categories.take(3)
        compose.waitForIdle()
        compose.onNodeWithText("Manga 30").assertExists()
        harness.events shouldContain "page 2"
    }
}
