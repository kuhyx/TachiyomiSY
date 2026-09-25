package eu.kanade.presentation.components

import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class DropdownMenuTest {
    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()

    @Test
    fun radioItemsShowTheirState() {
        compose.setContent {
            MaterialTheme {
                DropdownMenu(expanded = true, onDismissRequest = {}) {
                    RadioMenuItem(isChecked = true, onClick = { events += "on" }) { Text("On") }
                    RadioMenuItem(isChecked = false, modifier = Modifier, onClick = {}) { Text("Off") }
                }
            }
        }
        compose.onNodeWithContentDescription("Selected").assertExists()
        compose.onNodeWithContentDescription("Not selected").assertExists()
        compose.onNodeWithText("On").performClick()
        events shouldContainExactly listOf("on")
    }

    @Test
    fun explicitMenuArguments() {
        compose.setContent {
            MaterialTheme {
                DropdownMenu(
                    expanded = true,
                    onDismissRequest = {},
                    modifier = Modifier,
                    offset = DpOffset(0.dp, 0.dp),
                    scrollState = rememberScrollState(),
                    properties = PopupProperties(focusable = false),
                ) { Text("item") }
            }
        }
        compose.onNodeWithText("item").assertExists()
    }

    @Test
    fun nestedMenusOpenAndClose() {
        compose.setContent {
            MaterialTheme {
                DropdownMenu(expanded = true, onDismissRequest = {}) {
                    NestedMenuItem(text = { Text("Parent") }, children = { close ->
                        Text("Child", modifier = Modifier)
                        androidx.compose.material3.DropdownMenuItem(text = { Text("Close") }, onClick = close)
                    })
                }
            }
        }
        compose.onNodeWithText("Parent").performClick()
        compose.onNodeWithText("Child").assertExists()
        compose.onNodeWithText("Close").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Child").assertDoesNotExist()
    }
}
