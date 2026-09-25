package eu.kanade.presentation.library.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
internal class LibraryToolbarTest {
    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()

    private fun show(selected: Int = 0, syncExh: Boolean = true, sync: Boolean = true, count: Int? = 4) {
        compose.setContent {
            MaterialTheme {
                LibraryToolbar(
                    hasActiveFilters = syncExh,
                    selectedCount = selected,
                    title = LibraryToolbarTitle("Library", count),
                    onClickUnselectAll = { events += "unselect" },
                    onClickSelectAll = { events += "all" },
                    onClickInvertSelection = { events += "invert" },
                    onClickFilter = { events += "filter" },
                    onClickRefresh = { events += "refresh" },
                    onClickGlobalUpdate = { events += "global" },
                    onClickOpenRandomManga = { events += "random" },
                    onClickSyncNow = { events += "sync" },
                    onClickSyncExh = { events += "exh" }.takeIf { syncExh },
                    isSyncEnabled = sync,
                    searchQuery = null,
                    onSearchQueryChange = {},
                    scrollBehavior = null,
                )
            }
        }
        compose.waitForIdle()
    }

    private fun overflow(item: String) {
        compose.onNodeWithContentDescription("More options").performClick()
        compose.onNodeWithText(item).performClick()
    }

    @Test
    fun everyOverflowActionForwards() {
        show()
        compose.onNodeWithText("4").assertExists()
        compose.onNodeWithContentDescription("Filter").performClick()
        listOf("Update library", "Update category", "Open random entry", "Sync EH favorites", "Sync library")
            .forEach(::overflow)
        events shouldContainExactly listOf("filter", "global", "refresh", "random", "exh", "sync")
    }

    @Test
    @Config(qualifiers = "night")
    fun withoutSyncOrCount() {
        show(syncExh = false, sync = false, count = null)
        compose.onNodeWithContentDescription("More options").performClick()
        compose.onNodeWithText("Sync library").assertDoesNotExist()
        compose.onNodeWithText("Sync EH favorites").assertDoesNotExist()
    }

    @Test
    fun selectionToolbar() {
        show(selected = 2)
        compose.onNodeWithText("2").assertExists()
        compose.onNodeWithContentDescription("Select all").performClick()
        compose.onNodeWithContentDescription("Select inverse").performClick()
        compose.onNodeWithContentDescription("Cancel").performClick()
        events shouldContainExactly listOf("all", "invert", "unselect")
    }
}
