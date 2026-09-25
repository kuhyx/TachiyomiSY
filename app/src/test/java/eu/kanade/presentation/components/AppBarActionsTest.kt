package eu.kanade.presentation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import eu.kanade.presentation.util.tapOutsidePopup
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class AppBarActionsTest {
    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()

    @Test
    fun iconsOnlyHaveNoOverflow() {
        compose.setContent {
            MaterialTheme {
                Row {
                AppBarActions(
                    listOf(
                        AppBar.Action(title = "Star", icon = Icons.Outlined.Star, onClick = { events += "star" }),
                        AppBar.Action(
                            title = "Tinted",
                            icon = Icons.Outlined.Star,
                            iconTint = Color.Red,
                            onClick = {},
                            enabled = false,
                        ),
                    ),
                )
                }
            }
        }
        compose.onNodeWithContentDescription("More options").assertDoesNotExist()
        compose.onNodeWithContentDescription("Star").performClick()
        events shouldContainExactly listOf("star")
    }

    @Test
    fun overflowOpensAMenu() {
        compose.setContent {
            MaterialTheme {
                AppBarActions(
                    listOf(
                        AppBar.OverflowAction(title = "Refresh", onClick = { events += "refresh" }),
                        AppBar.OverflowAction(title = "Share", onClick = { events += "share" }),
                    ),
                )
            }
        }
        compose.onNodeWithContentDescription("More options").performClick()
        compose.onNodeWithText("Share").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Share").assertDoesNotExist()
        compose.onNodeWithContentDescription("More options").performClick()
        compose.onNodeWithContentDescription("More options").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Refresh").assertDoesNotExist()
        events shouldContainExactly listOf("share")
    }

    @Test
    fun overflowDismissesOutside() {
        compose.setContent {
            MaterialTheme {
                AppBarActions(listOf(AppBar.OverflowAction(title = "Refresh", onClick = {})))
            }
        }
        compose.onNodeWithContentDescription("More options").performClick()
        compose.onNodeWithText("Refresh").assertExists()
        compose.tapOutsidePopup()
        compose.onNodeWithText("Refresh").assertDoesNotExist()
    }
}
