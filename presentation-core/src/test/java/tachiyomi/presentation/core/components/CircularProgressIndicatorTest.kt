package tachiyomi.presentation.core.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class CircularProgressIndicatorTest {
    @get:Rule
    val compose = createComposeRule()

    private var progress by mutableFloatStateOf(0f)
    private var tick by mutableIntStateOf(0)

    private fun determinate(value: Float) =
        hasProgressBarRangeInfo(ProgressBarRangeInfo(current = value, range = 0f..1f))

    private fun indeterminate() = hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)

    @Test
    fun spinsIndeterminateAtZero() {
        compose.setContent { MaterialTheme { RotatingProgressIndicator(progress = { progress }) } }
        compose.onNode(indeterminate()).assertIsDisplayed()
    }

    @Test
    fun showsProgressOnceKnown() {
        progress = 0.5f
        compose.setContent {
            MaterialTheme { RotatingProgressIndicator(progress = { progress }, modifier = Modifier.testTag("spinner")) }
        }
        compose.onNodeWithTag("spinner").assertIsDisplayed()
        compose.onNode(indeterminate()).assertDoesNotExist()
        compose.onNode(determinate(0.5f)).assertIsDisplayed()
    }

    @Test
    fun fadesBetweenStates() {
        compose.setContent { MaterialTheme { RotatingProgressIndicator(progress = { progress }) } }
        compose.onNode(indeterminate()).assertIsDisplayed()
        compose.runOnIdle { progress = 0.25f }
        compose.waitForIdle()
        compose.onNode(indeterminate()).assertDoesNotExist()
        compose.onNode(determinate(0.25f)).assertIsDisplayed()
        compose.runOnIdle { progress = 0f }
        compose.waitForIdle()
        compose.onNode(indeterminate()).assertIsDisplayed()
    }

    @Test
    fun previewCyclesThroughProgress() {
        compose.setContent { RotatingProgressPreview() }
        compose.onNode(indeterminate()).assertIsDisplayed()
        repeat(4) {
            compose.onNodeWithText("change").performClick()
            compose.waitForIdle()
            compose.onNode(indeterminate()).assertDoesNotExist()
        }
        compose.onNodeWithText("change").performClick()
        compose.waitForIdle()
        compose.onNode(indeterminate()).assertIsDisplayed()
    }

    @Test
    fun unchangedRecomposeIsInert() {
        compose.setContent {
            MaterialTheme {
                Column {
                    Text(text = "tick $tick")
                    RotatingProgressIndicator(progress = { progress })
                }
            }
        }
        compose.runOnIdle { tick++ }
        compose.waitForIdle()
        compose.onNodeWithText("tick 1").assertIsDisplayed()
        compose.onNode(indeterminate()).assertIsDisplayed()
    }
}
