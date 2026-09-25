package eu.kanade.presentation.reader.appbars

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ExhUtilsTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(callbacks: ReaderBarCallbacks, visible: Boolean, enabled: Boolean = true) {
        compose.setContent {
            MaterialTheme {
                ExhUtils(
                    isVisible = visible,
                    onSetExhUtilsVisibility = { callbacks.events += "visible $it" },
                    backgroundColor = Color.Black,
                    autoScroll = callbacks.autoScroll(enabled),
                    pageActions = callbacks.pageActions(),
                )
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun visibleUtilitiesForwardEveryAction() {
        val callbacks = ReaderBarCallbacks()
        show(callbacks, visible = true)
        compose.onNodeWithText("Autoscroll").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("1")
        compose.onNodeWithText("Retry all").performClick()
        compose.onNodeWithText("Boost page").performClick()
        val help = compose.onAllNodesWithText("?")
        help[0].performClick()
        help[1].performClick()
        help[2].performClick()
        compose.onAllNodesWithText("Boost page")[0].assertExists()
        callbacks.events shouldContainExactly listOf(
            "autoscroll true",
            "frequency 13.0",
            "retry",
            "boost",
            "help",
            "retry help",
            "boost help",
        )
    }

    @Test
    fun hiddenUtilitiesToggleOpen() {
        val callbacks = ReaderBarCallbacks()
        show(callbacks, visible = false)
        compose.onNodeWithText("Retry all").assertDoesNotExist()
        compose.onNode(hasClickAction()).performClick()
        callbacks.events shouldContainExactly listOf("visible true")
    }

    @Test
    fun invalidFrequencyWarns() {
        show(ReaderBarCallbacks(), visible = true, enabled = false)
        compose.onNodeWithText("Invalid frequency").assertExists()
    }

    @Test
    fun previewRenders() {
        compose.setContent { MaterialTheme { ExhUtilsPreview() } }
        compose.onNodeWithText("Retry all").assertExists()
    }
}
