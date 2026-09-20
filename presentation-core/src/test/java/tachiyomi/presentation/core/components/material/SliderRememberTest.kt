package tachiyomi.presentation.core.components.material

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** [Slider] skipped on a plain recomposition, then recomposed one changed input at a time. */
@RunWith(RobolectricTestRunner::class)
internal class SliderRememberTest {
    @get:Rule
    val compose = createComposeRule()

    private var tick by mutableIntStateOf(0)
    private var value by mutableIntStateOf(1)
    private var range by mutableStateOf<IntProgression?>(null)
    private var steps by mutableStateOf<Int?>(null)
    private var isEnabled by mutableStateOf(true)
    private val onValueChange: (Int) -> Unit = { value = it }

    @Composable
    private fun Host(value: Int, range: IntProgression?, steps: Int?, enabled: Boolean) {
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.testTag("slider"),
            enabled = enabled,
            valueRange = range,
            steps = steps,
        )
    }

    private fun recompose() {
        tick += 1
        compose.waitForIdle()
    }

    @Test
    fun skippedThenChangedOneAtATime() {
        compose.setContent {
            MaterialTheme {
                Column {
                    Text("tick $tick")
                    Slider(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier.testTag("slider"),
                        enabled = isEnabled,
                        valueRange = range,
                        steps = steps,
                    )
                }
            }
        }
        compose.waitForIdle()
        recompose()
        compose.onNodeWithTag("slider").assertRangeInfoEquals(ProgressBarRangeInfo(current = 1f, range = 0f..1f))
        range = 0..10
        recompose()
        steps = 4
        recompose()
        isEnabled = false
        recompose()
        value = 6
        recompose()
        compose.onNodeWithTag("slider")
            .assertRangeInfoEquals(ProgressBarRangeInfo(current = 6f, range = 0f..10f, steps = 4))
    }

    @Test
    fun changedParametersThroughAHost() {
        compose.setContent {
            MaterialTheme {
                Column {
                    Text("tick $tick")
                    Host(value = value, range = range, steps = steps, enabled = isEnabled)
                }
            }
        }
        compose.waitForIdle()
        recompose()
        range = 0..4
        recompose()
        steps = 1
        recompose()
        isEnabled = false
        recompose()
        value = 2
        recompose()
        compose.onNodeWithTag("slider")
            .assertRangeInfoEquals(ProgressBarRangeInfo(current = 2f, range = 0f..4f, steps = 1))
    }
}
