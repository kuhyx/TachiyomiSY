package eu.kanade.presentation.manga.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private val Labels = listOf(
    "Bookmark chapter",
    "Unbookmark chapter",
    "Mark as read",
    "Mark as unread",
    "Mark previous as read",
    "Download",
    "Delete",
)

@RunWith(RobolectricTestRunner::class)
internal class MangaBottomActionMenuTest {
    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()
    private var visible by mutableStateOf(true)

    private fun setMenu(all: Boolean = true) {
        compose.setContent {
            MaterialTheme {
                MangaBottomActionMenu(
                    visible = visible,
                    onBookmarkClicked = { events += "bookmark" },
                    onRemoveBookmarkClicked = { events += "unbookmark" }.takeIf { all },
                    onMarkAsReadClicked = { events += "read" }.takeIf { all },
                    onMarkAsUnreadClicked = { events += "unread" }.takeIf { all },
                    onMarkPreviousAsReadClicked = { events += "previous" }.takeIf { all },
                    onDownloadClicked = { events += "download" }.takeIf { all },
                    onDeleteClicked = { events += "delete" }.takeIf { all },
                )
            }
        }
    }

    @Test
    fun everyActionForwards() {
        setMenu()
        Labels.forEach { compose.onNodeWithContentDescription(it).performClick() }
        events shouldContainExactly listOf("bookmark", "unbookmark", "read", "unread", "previous", "download", "delete")
    }

    @Test
    fun onlyGivenActionsShow() {
        setMenu(all = false)
        compose.onNodeWithContentDescription("Bookmark chapter").assertExists()
        compose.onNodeWithContentDescription("Delete").assertDoesNotExist()
    }

    @Test
    fun hiddenMenuShowsNothing() {
        visible = false
        setMenu()
        compose.onNodeWithContentDescription("Bookmark chapter").assertDoesNotExist()
    }

    @Test
    fun longPressLabelsForASecond() {
        setMenu()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithContentDescription("Delete").performTouchInput { longClick() }
        compose.mainClock.advanceTimeBy(ANIMATION_MS)
        compose.onNodeWithText("Delete").assertExists()
        compose.onNodeWithContentDescription("Download").performTouchInput { longClick() }
        compose.mainClock.advanceTimeBy(ANIMATION_MS)
        compose.onNodeWithText("Download").assertExists()
        compose.onNodeWithText("Delete").assertDoesNotExist()
        compose.mainClock.advanceTimeBy(RESET_MS)
        compose.onNodeWithText("Download").assertDoesNotExist()
        events shouldContainExactly emptyList()
    }

    private companion object {
        const val ANIMATION_MS = 400L
        const val RESET_MS = 1500L
    }
}
