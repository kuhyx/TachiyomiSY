package tachiyomi.presentation.core.components.material

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The elevation object as the receiver of its own animation, passed every way the compiler distinguishes. */
@RunWith(RobolectricTestRunner::class)
internal class ButtonElevationReceiverTest {

    @get:Rule
    val compose = createComposeRule()

    private var tick by mutableIntStateOf(0)
    private var elevation by mutableStateOf(ButtonDefaults.buttonElevation(defaultElevation = 1.dp))
    private var enabled by mutableStateOf(true)
    private var shadow: Dp = 0.dp

    @Composable
    private fun Host(elevation: ButtonElevation, enabled: Boolean) {
        val source = remember { MutableInteractionSource() }
        shadow = elevation.shadowElevation(enabled, source).value
        elevation.tonalElevation(enabled, source)
    }

    @Test
    fun receiverInstancesRecompose() {
        compose.setContent {
            MaterialTheme {
                Text(text = "tick $tick")
                Host(elevation = elevation, enabled = enabled)
            }
        }
        compose.onNodeWithText("tick 0").assertIsDisplayed()
        shadow shouldBe 1.dp
        val changes: List<() -> Unit> = listOf(
            { tick += 1 },
            { elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp) },
            { enabled = false },
            { elevation = ButtonDefaults.buttonElevation(disabledElevation = 5.dp) },
            { enabled = true },
            { tick += 1 },
        )
        changes.forEach { change ->
            compose.runOnIdle(change)
            compose.waitForIdle()
        }
        compose.onNodeWithText("tick 2").assertIsDisplayed()
    }
}
