package eu.kanade.presentation.updates

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import eu.kanade.presentation.util.PresentationKoin
import eu.kanade.tachiyomi.ui.updates.UpdatesScreenModel
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val PULL_STEPS = 30

@RunWith(RobolectricTestRunner::class)
internal class UpdatesScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = PresentationKoin()
    private val harness = UpdatesScreenHarness(compose)

    private val day = 86_400_000L
    private val twoDays by lazy {
        UpdatesScreenModel.State(
            isLoading = false,
            items = listOf(
                updatesItem(mangaId = 1L, dateFetch = 3 * day),
                updatesItem(mangaId = 2L, dateFetch = 3 * day),
                updatesItem(mangaId = 3L, dateFetch = day),
            ),
        )
    }

    @Before
    fun setUp() = koin.start()

    @After
    fun tearDown() = koin.stop()

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

    // A slow drag from the top of the list, well past the refresh threshold.
    private fun pull() {
        compose.onNodeWithText("Library last updated: Never").performTouchInput {
            down(center)
            repeat(PULL_STEPS) {
                advanceEventTime(16L)
                moveBy(Offset(0f, 20f))
            }
            up()
        }
    }

    @Test
    fun pullingRefreshesTheLibrary() {
        harness.updateStarts = false
        // Enough rows to scroll: a list that fits never starts the drag the refresh listens to.
        harness.show(twoDays.copy(items = (1L..30L).map { updatesItem(mangaId = it, dateFetch = day) }))
        pull()
        compose.waitForIdle()
        harness.updateStarts = true
        pull()
        compose.mainClock.advanceTimeBy(2_000L)
        compose.waitForIdle()
        harness.events shouldContainExactly listOf("update", "update")
    }
}
