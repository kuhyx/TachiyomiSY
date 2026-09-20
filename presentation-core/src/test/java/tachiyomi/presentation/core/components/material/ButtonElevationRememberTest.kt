package tachiyomi.presentation.core.components.material

import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
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

/**
 * The memoization arms of [ButtonElevation]'s effects: the same source on a plain
 * recomposition, a new source, and a source handed down as a changed parameter.
 */
@RunWith(RobolectricTestRunner::class)
internal class ButtonElevationRememberTest {
    @get:Rule
    val compose = createComposeRule()

    private val elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp, pressedElevation = 2.dp)
    private var source by mutableStateOf(MutableInteractionSource())
    private var isEnabled by mutableStateOf(true)
    private var tick by mutableIntStateOf(0)
    private var shadow: Dp = Dp.Unspecified

    @Composable
    private fun Host(enabled: Boolean, interactionSource: MutableInteractionSource) {
        shadow = elevation.shadowElevation(enabled, interactionSource).value
    }

    private fun press() {
        source.tryEmit(PressInteraction.Press(Offset.Zero)) shouldBe true
        compose.waitForIdle()
    }

    private fun recompose() {
        tick += 1
        compose.waitForIdle()
    }

    @Test
    fun sameSourceThenNewSource() {
        compose.setContent {
            MaterialTheme {
                Text("tick $tick")
                shadow = elevation.shadowElevation(isEnabled, source).value
            }
        }
        compose.waitForIdle()
        recompose()
        shadow shouldBe 1.dp
        press()
        shadow shouldBe 2.dp
        recompose()
        shadow shouldBe 2.dp
        source = MutableInteractionSource()
        compose.waitForIdle()
        shadow shouldBe 2.dp
        press()
        recompose()
        isEnabled = false
        compose.waitForIdle()
        shadow shouldBe 0.dp
        recompose()
        isEnabled = true
        compose.waitForIdle()
        shadow shouldBe 2.dp
    }

    @Test
    fun sourceAsAChangedParameter() {
        compose.setContent {
            MaterialTheme {
                Text("tick $tick")
                Host(enabled = isEnabled, interactionSource = source)
            }
        }
        compose.waitForIdle()
        recompose()
        shadow shouldBe 1.dp
        source = MutableInteractionSource()
        compose.waitForIdle()
        press()
        shadow shouldBe 2.dp
        recompose()
        isEnabled = false
        compose.waitForIdle()
        shadow shouldBe 0.dp
        isEnabled = true
        compose.waitForIdle()
        source = MutableInteractionSource()
        compose.waitForIdle()
        shadow shouldBe 2.dp
    }

    @Test
    fun sameLastInteractionRecomposes() {
        compose.setContent {
            MaterialTheme {
                Text("tick $tick")
                shadow = elevation.shadowElevation(isEnabled, source).value
            }
        }
        compose.waitForIdle()
        val enter = HoverInteraction.Enter()
        source.tryEmit(enter) shouldBe true
        compose.waitForIdle()
        val press = PressInteraction.Press(Offset.Zero)
        source.tryEmit(press) shouldBe true
        compose.waitForIdle()
        shadow shouldBe 2.dp
        source.tryEmit(HoverInteraction.Exit(enter)) shouldBe true
        compose.waitForIdle()
        shadow shouldBe 2.dp
        val second = PressInteraction.Press(Offset.Zero)
        source.tryEmit(second) shouldBe true
        compose.waitForIdle()
        source.tryEmit(PressInteraction.Cancel(second)) shouldBe true
        compose.waitForIdle()
        shadow shouldBe 2.dp
        source.tryEmit(PressInteraction.Release(press)) shouldBe true
        compose.waitForIdle()
        shadow shouldBe 1.dp
    }

    @Test
    fun throughAButton() {
        compose.setContent {
            MaterialTheme {
                Column {
                    Text("tick $tick")
                    Button(onClick = {}, enabled = isEnabled, interactionSource = source) { Text("b") }
                }
            }
        }
        compose.waitForIdle()
        recompose()
        press()
        recompose()
        source = MutableInteractionSource()
        compose.waitForIdle()
        isEnabled = false
        compose.waitForIdle()
        isEnabled = true
        compose.waitForIdle()
        compose.onNodeWithText("b").assertIsDisplayed()
    }
}
