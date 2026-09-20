package tachiyomi.presentation.core.components.material

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class SliderTest {
    @get:Rule
    val compose = createComposeRule()

    private val changes = mutableListOf<Int>()
    private var finished = 0
    private var value by mutableIntStateOf(0)

    @Test
    fun defaultsAreABinaryRange() {
        compose.setContent {
            MaterialTheme {
                Slider(
                    value = value,
                    onValueChange = { value = it },
                    modifier = Modifier.testTag("slider"),
                )
            }
        }
        val slider = compose.onNodeWithTag("slider")
        slider.assertRangeInfoEquals(ProgressBarRangeInfo(current = 0f, range = 0f..1f, steps = 0))
        slider.performSemanticsAction(SemanticsActions.SetProgress) { it(1f) }
        value shouldBe 1
        slider.assertRangeInfoEquals(ProgressBarRangeInfo(current = 1f, range = 0f..1f, steps = 0))
    }

    @Test
    fun rangeWithoutStepsIsOnePerValue() {
        value = 3
        compose.setContent {
            MaterialTheme {
                Slider(
                    value = value,
                    onValueChange = {
                        changes += it
                        value = it
                    },
                    modifier = Modifier.testTag("slider").fillMaxWidth(),
                    valueRange = 0..10,
                    onValueChangeFinished = { finished += 1 },
                )
            }
        }
        val slider = compose.onNodeWithTag("slider")
        slider.assertRangeInfoEquals(ProgressBarRangeInfo(current = 3f, range = 0f..10f, steps = 9))
        slider.performSemanticsAction(SemanticsActions.SetProgress) { it(7f) }
        changes shouldBe listOf(7)
        finished shouldBe 1
        slider.performTouchInput { swipeRight(startX = centerX) }
        changes.shouldNotBeEmpty()
        value shouldBe 10
    }

    @Test
    fun everyParameterGiven() {
        value = 2
        compose.setContent {
            MaterialTheme {
                Slider(
                    value = value,
                    onValueChange = { value = it },
                    modifier = Modifier.testTag("slider").fillMaxWidth(),
                    enabled = true,
                    valueRange = 0..10,
                    steps = 4,
                    onValueChangeFinished = { finished += 1 },
                    colors = SliderDefaults.colors(),
                    interactionSource = remember { MutableInteractionSource() },
                    thumb = { Box(Modifier.size(20.dp).testTag("thumb")) },
                    track = { _ -> Box(Modifier.fillMaxWidth().height(4.dp).testTag("track")) },
                )
            }
        }
        compose.onNodeWithTag("thumb", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("track", useUnmergedTree = true).assertExists()
        val slider = compose.onNodeWithTag("slider")
        slider.assertRangeInfoEquals(ProgressBarRangeInfo(current = 2f, range = 0f..10f, steps = 4))
        slider.performSemanticsAction(SemanticsActions.SetProgress) { it(6f) }
        value shouldBe 6
        finished shouldBe 1
    }

    @Test
    fun disabledSliderUsesDefaultSlots() {
        value = 1
        compose.setContent {
            MaterialTheme {
                Slider(
                    value = value,
                    onValueChange = { value = it },
                    modifier = Modifier.testTag("slider"),
                    enabled = false,
                    valueRange = 0..4,
                    steps = 3,
                )
            }
        }
        compose.onNodeWithTag("slider")
            .assertIsNotEnabled()
            .assertRangeInfoEquals(ProgressBarRangeInfo(current = 1f, range = 0f..4f, steps = 3))
    }
}
