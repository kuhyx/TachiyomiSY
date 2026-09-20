package tachiyomi.presentation.core.util

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val FIELD = "field"

/**
 * Recomposes [showSoftKeyboard] and [clearFocusOnSoftKeyboardHide] with the same, then new,
 * argument instances (held in state and passed through a parameterised host).
 */
@RunWith(RobolectricTestRunner::class)
internal class ModifierBranchTest {
    @get:Rule
    val compose = createComposeRule()

    private var cleared = 0
    private var show by mutableStateOf(true)
    private var onCleared: () -> Unit by mutableStateOf<() -> Unit>({ cleared++ })
    private var tick by mutableIntStateOf(0)

    @Composable
    private fun Host(show: Boolean, onCleared: () -> Unit) {
        BasicTextField(
            state = rememberTextFieldState("x"),
            modifier = Modifier.showSoftKeyboard(show).clearFocusOnSoftKeyboardHide(onCleared).testTag(FIELD),
        )
    }

    private fun idle() = compose.waitForIdle()

    private fun swapArguments() {
        compose.onNodeWithTag(FIELD).assertIsFocused()
        compose.runOnIdle { tick++ }
        idle()
        compose.onNodeWithTag(FIELD).assertIsFocused()
        compose.runOnIdle { onCleared = { cleared += 10 } }
        idle()
        compose.runOnIdle { show = false }
        idle()
        compose.runOnIdle { show = true }
        idle()
        compose.onNodeWithTag(FIELD).assertIsFocused()
        cleared shouldBe 0
    }

    @Test
    fun stateHeldArgumentsRecompose() {
        compose.setContent {
            MaterialTheme {
                Column {
                    Text(text = "tick $tick")
                    BasicTextField(
                        state = rememberTextFieldState("x"),
                        modifier = Modifier
                            .showSoftKeyboard(show)
                            .clearFocusOnSoftKeyboardHide(onCleared)
                            .testTag(FIELD),
                    )
                }
            }
        }
        swapArguments()
        compose.onNodeWithText("tick 1").assertIsDisplayed()
    }

    @Test
    fun hostParametersRecompose() {
        compose.setContent {
            MaterialTheme {
                Column {
                    Text(text = "tick $tick")
                    Host(show = show, onCleared = onCleared)
                }
            }
        }
        swapArguments()
    }

    @Test
    fun showOffThenOnFocusesAgain() {
        show = false
        compose.setContent { MaterialTheme { Host(show = show, onCleared = onCleared) } }
        compose.onNodeWithTag(FIELD).assertIsNotFocused()
        compose.runOnIdle { show = true }
        idle()
        compose.onNodeWithTag(FIELD).assertIsFocused()
    }
}
