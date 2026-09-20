package tachiyomi.presentation.core.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val WHEEL_TAG = "wheel"

/** The two public pickers: defaults, explicit options, scrolling and (for numbers) the tap target. */
@RunWith(RobolectricTestRunner::class)
internal class WheelPickerTest {
    @get:Rule
    val compose = createComposeRule()

    private val numbers: List<Number> = (0..9).toList()
    private val words = listOf("alpha", "beta", "gamma", "delta")
    private val selected = mutableListOf<Int>()
    private var tick by mutableIntStateOf(0)

    private fun setNumberPicker(startIndex: Int = 0, size: DpSize = DpSize(128.dp, 128.dp)) {
        compose.setContent {
            MaterialTheme {
                WheelNumberPicker(
                    items = numbers,
                    modifier = Modifier.testTag(WHEEL_TAG),
                    startIndex = startIndex,
                    size = size,
                    onSelectionChanged = { selected += it },
                    backgroundContent = null,
                )
            }
        }
    }

    @Test
    fun numberPickerDefaultsToFirst() {
        compose.setContent { MaterialTheme { WheelNumberPicker(items = numbers) } }
        compose.onNodeWithText("0").assertIsDisplayed()
        compose.onNodeWithText("1").assertIsDisplayed()
        compose.onNodeWithText("5").assertDoesNotExist()
        selected.isEmpty() shouldBe true
    }

    @Test
    fun numberPickerHonoursOptions() {
        setNumberPicker(startIndex = 4, size = DpSize(200.dp, 150.dp))
        compose.onNodeWithText("4").assertIsDisplayed()
        compose.onNodeWithTag(WHEEL_TAG).assertWidthIsEqualTo(200.dp)
        selected shouldBe listOf(4)
    }

    @Test
    fun swipeMovesSelection() {
        setNumberPicker()
        compose.onNodeWithTag(WHEEL_TAG).performTouchInput { swipeUp() }
        compose.waitForIdle()
        selected.last() shouldBeGreaterThan 0
    }

    @Test
    fun numberPickerOpensInputOnTap() {
        setNumberPicker()
        compose.onNodeWithText("0").performClick()
        compose.waitForIdle()
        compose.onNode(hasSetTextAction()).assertIsDisplayed()
        compose.onNodeWithText("1").assertDoesNotExist()
    }

    @Test
    fun textPickerDefaultsToFirstItem() {
        compose.setContent { MaterialTheme { WheelTextPicker(items = words) } }
        compose.onNodeWithText("alpha").assertIsDisplayed()
        compose.onNodeWithText("beta").assertIsDisplayed()
        compose.onNodeWithText("delta").assertDoesNotExist()
    }

    @Test
    fun textPickerHasNoTapInput() {
        compose.setContent { MaterialTheme { WheelTextPicker(items = words) } }
        compose.onNodeWithText("alpha").performClick()
        compose.waitForIdle()
        compose.onNode(hasSetTextAction()).assertDoesNotExist()
        compose.onNodeWithText("alpha").assertIsDisplayed()
    }

    @Test
    fun textPickerHonoursOptions() {
        compose.setContent {
            MaterialTheme {
                WheelTextPicker(
                    items = words,
                    modifier = Modifier.testTag(WHEEL_TAG),
                    startIndex = 2,
                    size = DpSize(180.dp, 90.dp),
                    onSelectionChanged = { selected += it },
                    backgroundContent = null,
                )
            }
        }
        compose.onNodeWithText("gamma").assertIsDisplayed()
        compose.onNodeWithTag(WHEEL_TAG).assertWidthIsEqualTo(180.dp)
        selected shouldBe listOf(2)
    }

    @Test
    fun unchangedRecomposeIsInert() {
        compose.setContent {
            MaterialTheme {
                Column {
                    Text(text = "tick $tick")
                    WheelNumberPicker(items = numbers, onSelectionChanged = { selected += it })
                    WheelTextPicker(items = words, onSelectionChanged = { selected += it })
                }
            }
        }
        compose.runOnIdle { tick++ }
        compose.waitForIdle()
        compose.onNodeWithText("tick 1").assertIsDisplayed()
        compose.onNodeWithText("0").assertIsDisplayed()
        compose.onNodeWithText("alpha").assertIsDisplayed()
        selected shouldBe listOf(0, 0)
    }
}
