package eu.kanade.presentation.updates

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import eu.kanade.presentation.util.PresentationKoin
import eu.kanade.tachiyomi.ui.updates.UpdatesScreenModel
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class UpdatesScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = PresentationKoin()
    private val harness = UpdatesScreenHarness(compose)

    @Before
    fun setUp() = koin.start()

    @After
    fun tearDown() = koin.stop()

    private val day = 86_400_000L
    private val twoDays = UpdatesScreenModel.State(
        isLoading = false,
        items = listOf(
            updatesItem(mangaId = 1L, dateFetch = 3 * day),
            updatesItem(mangaId = 2L, dateFetch = 3 * day),
            updatesItem(mangaId = 3L, dateFetch = day),
        ),
    )

    @Test
    fun loadingShowsTheAppBar() {
        harness.show(UpdatesScreenModel.State(), hasActiveFilters = true)
        compose.onNodeWithText("Updates").assertExists()
    }

    @Test
    fun emptyShowsTheMessage() {
        harness.show(UpdatesScreenModel.State(isLoading = false))
        compose.onNodeWithText("No recent updates").assertExists()
    }

    @Test
    fun appBarActionsForward() {
        harness.show(UpdatesScreenModel.State(isLoading = false))
        compose.onNodeWithContentDescription("Filter").performClick()
        compose.onNodeWithContentDescription("View Upcoming Updates").performClick()
        compose.onNodeWithContentDescription("Update library").performClick()
        harness.events shouldContainExactly listOf("filter", "calendar", "update")
    }

    @Test
    fun rowsOpenAndShowCovers() {
        harness.show(twoDays)
        compose.onNodeWithText("Library last updated: Never").assertExists()
        compose.onNodeWithText("Chapter 3").performClick()
        compose.onNodeWithText("Manga 1").performClick()
        harness.events shouldContainExactly listOf("open 3", "open 1")
    }

    @Test
    fun pullToRefreshStartsAnUpdate() {
        harness.show(twoDays)
        compose.onNodeWithText("Chapter 1").performTouchInput { swipeDown() }
        compose.mainClock.advanceTimeBy(2_000L)
        compose.waitForIdle()
        harness.events shouldContainExactly listOf("update")
    }

    @Test
    fun pullToRefreshCanBeRefused() {
        harness.updateStarts = false
        harness.show(twoDays)
        compose.onNodeWithText("Chapter 1").performTouchInput { swipeDown() }
        compose.waitForIdle()
        harness.events shouldContainExactly listOf("update")
    }

    @Test
    fun selectionModeActions() {
        val state = twoDays.copy(items = twoDays.items.map { it.copy(selected = it.update.mangaId == 1L) })
        harness.show(state)
        compose.onNodeWithText("1").assertExists()
        compose.onNodeWithContentDescription("Select all").performClick()
        compose.onNodeWithContentDescription("Select inverse").performClick()
        compose.onNodeWithContentDescription("Cancel").performClick()
        compose.onNodeWithText("Chapter 2").performClick()
        harness.events shouldContainExactly listOf("selectAll true", "invert", "selectAll false", "select 2 true false")
    }

    @Test
    fun backLeavesSelectionMode() {
        harness.show(twoDays.copy(items = twoDays.items.map { it.copy(selected = true) }))
        compose.runOnIdle { harness.back.pressBack() }
        harness.events shouldContainExactly listOf("selectAll false")
    }
}
