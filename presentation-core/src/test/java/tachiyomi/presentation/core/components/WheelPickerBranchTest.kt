package tachiyomi.presentation.core.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Recomposes the pickers with the same, then new, argument instances (held in state and passed
 * through a parameterised host), both on the wheel and with the manual input open, so every
 * memoized lambda takes every path.
 */
@RunWith(RobolectricTestRunner::class)
internal class WheelPickerBranchTest {
    @get:Rule
    val compose = createComposeRule()

    private val selected = mutableListOf<Int>()
    private var items: List<Number> by mutableStateOf((0..9).toList())
    private var words by mutableStateOf(listOf("alpha", "beta", "gamma"))
    private var size by mutableStateOf(DpSize(128.dp, 128.dp))
    private var onSelect: (Int) -> Unit by mutableStateOf<(Int) -> Unit>({ selected += it })
    private var tick by mutableIntStateOf(0)

    @Composable
    private fun Host(items: List<Number>, size: DpSize, onSelect: (Int) -> Unit, tick: Int) {
        Text(text = "host $tick")
        WheelNumberPicker(items = items, size = size, onSelectionChanged = onSelect)
    }

    @Composable
    private fun TextHost(words: List<String>, size: DpSize, onSelect: (Int) -> Unit, tick: Int) {
        Text(text = "host $tick")
        WheelTextPicker(items = words, size = size, onSelectionChanged = onSelect)
    }

    private fun idle() = compose.waitForIdle()

    private fun swapArgumentsThroughInput() {
        compose.runOnIdle { tick++ }
        idle()
        compose.onNodeWithText("0").performClick()
        idle()
        compose.onNode(hasSetTextAction()).assertIsDisplayed()
        compose.runOnIdle { tick++ }
        idle()
        compose.runOnIdle { onSelect = { selected += it + 100 } }
        idle()
        compose.runOnIdle { items = (10..19).toList() }
        idle()
        compose.runOnIdle { size = DpSize(150.dp, 150.dp) }
        idle()
        compose.onNode(hasSetTextAction()).performTextReplacement("13")
        compose.onNode(hasSetTextAction()).performImeAction()
        idle()
        compose.onNode(hasSetTextAction()).assertDoesNotExist()
        compose.onNodeWithText("13").assertIsDisplayed()
        selected shouldContain 103
        compose.runOnIdle { tick++ }
        idle()
        compose.onNodeWithText("13").assertIsDisplayed()
    }

    @Test
    fun stateHeldArgumentsRecompose() {
        compose.setContent {
            MaterialTheme {
                Column {
                    Text(text = "tick $tick")
                    WheelNumberPicker(items = items, size = size, onSelectionChanged = onSelect)
                }
            }
        }
        swapArgumentsThroughInput()
        compose.onNodeWithText("tick 3").assertIsDisplayed()
    }

    @Test
    fun hostParametersRecompose() {
        compose.setContent {
            MaterialTheme {
                Column {
                    Host(items = items, size = size, onSelect = onSelect, tick = tick)
                }
            }
        }
        swapArgumentsThroughInput()
    }

    @Test
    fun textPickerArgumentsRecompose() {
        compose.setContent {
            MaterialTheme {
                Column {
                    Text(text = "tick $tick")
                    WheelTextPicker(items = words, size = size, onSelectionChanged = onSelect)
                    TextHost(words = words, size = size, onSelect = onSelect, tick = tick)
                }
            }
        }
        compose.runOnIdle { tick++ }
        idle()
        compose.runOnIdle { onSelect = { selected += it + 100 } }
        idle()
        compose.runOnIdle { words = listOf("delta", "epsilon") }
        idle()
        compose.runOnIdle { size = DpSize(150.dp, 150.dp) }
        idle()
        compose.onNodeWithText("alpha").assertDoesNotExist()
        selected shouldContain 100
        selected.first() shouldBe 0
    }
}
