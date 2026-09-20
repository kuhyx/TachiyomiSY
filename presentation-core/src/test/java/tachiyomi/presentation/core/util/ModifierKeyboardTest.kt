package tachiyomi.presentation.core.util

import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.requestFocus
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val FIELD = "field"

/** [showSoftKeyboard] and [clearFocusOnSoftKeyboardHide], with the IME simulated through window insets. */
@RunWith(RobolectricTestRunner::class)
internal class ModifierKeyboardTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var cleared = 0
    private var tick by mutableIntStateOf(0)

    private fun composeView(): View {
        val content = compose.activity.findViewById<ViewGroup>(android.R.id.content)
        return (content.getChildAt(0) as ViewGroup).getChildAt(0)
    }

    private fun dispatchImeInsetsToComposeView(visible: Boolean) {
        compose.runOnIdle {
            val insets = WindowInsetsCompat.Builder().setVisible(WindowInsetsCompat.Type.ime(), visible).build()
            ViewCompat.dispatchApplyWindowInsets(composeView(), insets)
        }
        compose.waitForIdle()
    }

    private fun setField(modifier: @Composable () -> Modifier) {
        compose.setContent {
            MaterialTheme {
                Column {
                    Text(text = "tick $tick")
                    BasicTextField(state = rememberTextFieldState("x"), modifier = modifier().testTag(FIELD))
                }
            }
        }
    }

    @Test
    fun showKeyboardFocusesAtStart() {
        setField { Modifier.showSoftKeyboard(true) }
        compose.onNodeWithTag(FIELD).assertIsFocused()
    }

    @Test
    fun showKeyboardOffLeavesFocus() {
        setField { Modifier.showSoftKeyboard(false) }
        compose.onNodeWithTag(FIELD).assertIsNotFocused()
    }

    @Test
    fun showKeyboardOncePerSavedState() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            MaterialTheme {
                BasicTextField(
                    state = rememberTextFieldState("x"),
                    modifier = Modifier.showSoftKeyboard(true).testTag(FIELD),
                )
            }
        }
        compose.onNodeWithTag(FIELD).assertIsFocused()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag(FIELD).assertIsNotFocused()
    }

    @Test
    fun clearsFocusWhenKeyboardHides() {
        setField { Modifier.clearFocusOnSoftKeyboardHide { cleared++ } }
        compose.onNodeWithTag(FIELD).requestFocus()
        compose.onNodeWithTag(FIELD).assertIsFocused()
        dispatchImeInsetsToComposeView(true)
        compose.onNodeWithTag(FIELD).assertIsFocused()
        dispatchImeInsetsToComposeView(false)
        compose.onNodeWithTag(FIELD).assertIsNotFocused()
        cleared shouldBe 1
    }

    @Test
    fun clearsFocusWithoutCallback() {
        setField { Modifier.clearFocusOnSoftKeyboardHide() }
        compose.onNodeWithTag(FIELD).requestFocus()
        dispatchImeInsetsToComposeView(true)
        dispatchImeInsetsToComposeView(false)
        compose.onNodeWithTag(FIELD).assertIsNotFocused()
        cleared shouldBe 0
    }

    @Test
    fun keepsFocusUntilKeyboardShown() {
        setField { Modifier.clearFocusOnSoftKeyboardHide { cleared++ } }
        compose.onNodeWithTag(FIELD).requestFocus()
        dispatchImeInsetsToComposeView(false)
        compose.onNodeWithTag(FIELD).assertIsFocused()
        cleared shouldBe 0
    }

    @Test
    fun refocusForgetsEarlierKeyboard() {
        setField { Modifier.clearFocusOnSoftKeyboardHide { cleared++ } }
        compose.onNodeWithTag(FIELD).requestFocus()
        dispatchImeInsetsToComposeView(true)
        dispatchImeInsetsToComposeView(false)
        cleared shouldBe 1
        compose.onNodeWithTag(FIELD).requestFocus()
        compose.onNodeWithTag(FIELD).assertIsFocused()
        dispatchImeInsetsToComposeView(true)
        dispatchImeInsetsToComposeView(false)
        compose.onNodeWithTag(FIELD).assertIsNotFocused()
        cleared shouldBe 2
    }

    @Test
    fun unchangedRecomposeIsInert() {
        setField { Modifier.showSoftKeyboard(true).clearFocusOnSoftKeyboardHide { cleared++ } }
        compose.runOnIdle { tick++ }
        compose.waitForIdle()
        compose.onNodeWithText("tick 1").assertIsDisplayed()
        compose.onNodeWithTag(FIELD).assertIsFocused()
        cleared shouldBe 0
    }
}
