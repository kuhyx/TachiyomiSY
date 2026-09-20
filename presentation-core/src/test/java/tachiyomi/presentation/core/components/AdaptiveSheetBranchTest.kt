package tachiyomi.presentation.core.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val SHEET_TAG = "sheet"

/**
 * Recomposes the phone sheet with the same, then new, argument instances (held in state and
 * passed through a parameterised host) so its memoized lambdas take every path.
 */
@RunWith(RobolectricTestRunner::class)
internal class AdaptiveSheetBranchTest {
    @get:Rule
    val compose = createComposeRule()

    private var dismissed = 0
    private var onDismiss: () -> Unit by mutableStateOf<() -> Unit>({ dismissed++ })
    private var swipe by mutableStateOf(true)
    private var tick by mutableIntStateOf(0)

    @Composable
    private fun Host(swipe: Boolean, onDismiss: () -> Unit) {
        AdaptiveSheet(isTabletUi = false, enableSwipeDismiss = swipe, onDismissRequest = onDismiss) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp).testTag(SHEET_TAG))
        }
    }

    private fun idle() = compose.waitForIdle()

    private fun tapOutside() {
        compose.onRoot().performTouchInput { click(Offset(2f, 2f)) }
        idle()
    }

    private fun swapArgumentsThenDismiss() {
        compose.runOnIdle { tick++ }
        idle()
        compose.onNodeWithTag(SHEET_TAG, useUnmergedTree = true).assertIsDisplayed()
        compose.runOnIdle { onDismiss = { dismissed += 10 } }
        idle()
        compose.runOnIdle { swipe = false }
        idle()
        compose.runOnIdle { swipe = true }
        idle()
        tapOutside()
        // The dismissal effect is keyed on the drag state only, so it keeps the first callback.
        dismissed shouldBe 1
    }

    private fun dragSheetSlowlyBy(pixels: Int) {
        compose.onNodeWithTag(SHEET_TAG, useUnmergedTree = true).performTouchInput {
            // One pixel per frame (about 62 px/s) keeps the fling below the 125 dp/s velocity threshold.
            down(center)
            repeat(pixels) { moveBy(Offset(0f, 1f)) }
            up()
        }
        idle()
    }

    @Test
    fun slowDragUsesPositionThreshold() {
        compose.setContent { MaterialTheme { Host(swipe = true, onDismiss = onDismiss) } }
        dragSheetSlowlyBy(30)
        dismissed shouldBe 0
        dragSheetSlowlyBy(120)
        dismissed shouldBe 1
    }

    @Test
    fun stateHeldArgumentsRecompose() {
        compose.setContent {
            MaterialTheme {
                Box {
                    Text(text = "tick $tick")
                    AdaptiveSheet(isTabletUi = false, enableSwipeDismiss = swipe, onDismissRequest = onDismiss) {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp).testTag(SHEET_TAG))
                    }
                }
            }
        }
        swapArgumentsThenDismiss()
        compose.onNodeWithText("tick 1").assertIsDisplayed()
    }

    @Test
    fun hostParametersRecompose() {
        compose.setContent {
            MaterialTheme {
                Box {
                    Text(text = "tick $tick")
                    Host(swipe = swipe, onDismiss = onDismiss)
                }
            }
        }
        swapArgumentsThenDismiss()
    }
}
