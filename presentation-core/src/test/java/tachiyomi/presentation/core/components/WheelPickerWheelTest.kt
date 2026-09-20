package tachiyomi.presentation.core.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextReplacement
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The manual text entry that [WheelNumberPicker] swaps in when its wheel is tapped. */
@RunWith(RobolectricTestRunner::class)
internal class WheelPickerWheelTest {
    @get:Rule
    val compose = createComposeRule()

    private val numbers: List<Number> = (0..9).toList()
    private val selected = mutableListOf<Int>()

    private fun openInput(startIndex: Int = 0) {
        compose.setContent {
            MaterialTheme {
                WheelNumberPicker(
                    items = numbers,
                    startIndex = startIndex,
                    onSelectionChanged = { selected += it },
                )
            }
        }
        compose.onNodeWithText("$startIndex").performClick()
        compose.waitForIdle()
    }

    @Test
    fun inputStartsWithCurrentItem() {
        openInput(startIndex = 2)
        compose.onNode(hasSetTextAction()).assertTextEquals("2")
        compose.onNode(hasSetTextAction()).assertIsFocused()
        compose.onNodeWithText("3").assertDoesNotExist()
    }

    @Test
    fun typedValueSelectsMatchingItem() {
        openInput()
        compose.onNode(hasSetTextAction()).performTextReplacement("3")
        compose.onNode(hasSetTextAction()).performImeAction()
        compose.waitForIdle()
        compose.onNode(hasSetTextAction()).assertDoesNotExist()
        compose.onNodeWithText("3").assertIsDisplayed()
        selected.last() shouldBe 3
    }

    @Test
    fun unknownValueJustClosesInput() {
        openInput()
        compose.onNode(hasSetTextAction()).performTextReplacement("x")
        compose.onNode(hasSetTextAction()).performImeAction()
        compose.waitForIdle()
        compose.onNode(hasSetTextAction()).assertDoesNotExist()
        compose.onNodeWithText("0").assertIsDisplayed()
        selected shouldBe listOf(0)
    }

    @Test
    fun emptyValueJustClosesInput() {
        openInput()
        compose.onNode(hasSetTextAction()).performTextReplacement("")
        compose.onNode(hasSetTextAction()).performImeAction()
        compose.waitForIdle()
        compose.onNode(hasSetTextAction()).assertDoesNotExist()
        compose.onNodeWithText("0").assertIsDisplayed()
        selected shouldBe listOf(0)
    }
}
