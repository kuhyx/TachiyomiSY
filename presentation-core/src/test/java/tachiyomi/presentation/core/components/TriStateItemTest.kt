package tachiyomi.presentation.core.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.preference.TriState

@Composable
private fun TriStateItemHost(state: TriState, onClick: ((TriState) -> Unit)?) {
    TriStateItem(label = "Filter", state = state, onClick = onClick)
}

@RunWith(RobolectricTestRunner::class)
internal class TriStateItemTest {
    @get:Rule
    val compose = createComposeRule()

    private var received: TriState? = null

    @Test
    fun disabledStateAdvancesToIs() {
        compose.setContent {
            MaterialTheme {
                TriStateItem(label = "Filter", state = TriState.DISABLED, onClick = { next -> received = next })
            }
        }
        compose.onNodeWithText("Filter").assertIsEnabled().performClick()
        compose.runOnIdle { received shouldBe TriState.ENABLED_IS }
    }

    @Test
    fun isStateAdvancesToNot() {
        compose.setContent {
            MaterialTheme {
                TriStateItem(
                    label = "Filter",
                    state = TriState.ENABLED_IS,
                    enabled = true,
                    onClick = { next -> received = next },
                )
            }
        }
        compose.onNodeWithText("Filter").performClick()
        compose.runOnIdle { received shouldBe TriState.ENABLED_NOT }
    }

    @Test
    fun notStateAdvancesToDisabled() {
        compose.setContent {
            MaterialTheme {
                TriStateItem(label = "Filter", state = TriState.ENABLED_NOT, onClick = { next -> received = next })
            }
        }
        compose.onNodeWithText("Filter").performClick()
        compose.runOnIdle { received shouldBe TriState.DISABLED }
    }

    @Test
    fun nullClickIsInert() {
        compose.setContent {
            MaterialTheme { TriStateItem(label = "Filter", state = TriState.ENABLED_IS, onClick = null) }
        }
        val row = compose.onNodeWithText("Filter").assertIsNotEnabled()
        // The click semantics survive the disabled flag; invoking it runs the null-safe branch.
        row.performSemanticsAction(SemanticsActions.OnClick)
        compose.runOnIdle { received.shouldBeNull() }
    }

    @Test
    fun nullClickOnDisabledState() {
        compose.setContent {
            MaterialTheme { TriStateItem(label = "Filter", state = TriState.DISABLED, onClick = null) }
        }
        compose.onNodeWithText("Filter").assertIsNotEnabled()
    }

    @Test
    fun disabledFlagIsInert() {
        compose.setContent {
            MaterialTheme {
                TriStateItem(
                    label = "Filter",
                    state = TriState.ENABLED_NOT,
                    enabled = false,
                    onClick = { next -> received = next },
                )
            }
        }
        compose.onNodeWithText("Filter").assertIsNotEnabled().performClick()
        compose.runOnIdle { received.shouldBeNull() }
    }

    @Test
    fun changedInputsRebuildTheClick() {
        var state by mutableStateOf(TriState.DISABLED)
        var handler by mutableStateOf<((TriState) -> Unit)?>(null)
        compose.setContent { MaterialTheme { TriStateItemHost(state = state, onClick = handler) } }
        compose.waitForIdle()
        state = TriState.ENABLED_IS
        compose.waitForIdle()
        handler = { next -> received = next }
        compose.waitForIdle()
        compose.onNodeWithText("Filter").assertIsEnabled().performClick()
        compose.runOnIdle { received shouldBe TriState.ENABLED_NOT }
    }
}
