package eu.kanade.presentation.webview.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class IgneousDialogTest {
    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()

    @Test
    fun okTrimsTheCookie() {
        compose.setContent {
            MaterialTheme {
                IgneousDialog(onDismissRequest = { events += "dismiss" }, onIgneousSet = { events += "set $it" })
            }
        }
        compose.onNodeWithText("Custom igneous cookie").assertExists()
        compose.onNode(hasSetTextAction()).performTextInput("  abc  ")
        compose.onNodeWithText("OK").performClick()
        compose.onNodeWithText("Cancel").performClick()
        events shouldContainExactly listOf("set abc", "dismiss", "dismiss")
    }
}
