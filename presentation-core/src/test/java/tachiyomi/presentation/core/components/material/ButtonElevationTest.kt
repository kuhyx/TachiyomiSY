package tachiyomi.presentation.core.components.material

import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Five distinct values so [ButtonElevation]'s reverse lookup can tell every state apart. */
@RunWith(RobolectricTestRunner::class)
internal class ButtonElevationTest {
    @get:Rule
    val compose = createComposeRule()

    private val elevation = ButtonDefaults.buttonElevation(
        defaultElevation = 1.dp,
        pressedElevation = 2.dp,
        focusedElevation = 3.dp,
        hoveredElevation = 4.dp,
        disabledElevation = 5.dp,
    )
    private val source = MutableInteractionSource()
    private val enabled = mutableStateOf(true)
    private var shadow: Dp = Dp.Unspecified
    private var tonal: Dp = Dp.Unspecified

    private fun composeShadow() {
        compose.setContent {
            MaterialTheme { shadow = elevation.shadowElevation(enabled.value, source).value }
        }
        compose.waitForIdle()
    }

    private fun emit(interaction: Interaction) {
        source.tryEmit(interaction) shouldBe true
        compose.waitForIdle()
    }

    @Test
    fun restsAtDefault() {
        composeShadow()
        shadow shouldBe 1.dp
    }

    @Test
    fun pressThenRelease() {
        composeShadow()
        val press = PressInteraction.Press(Offset.Zero)
        emit(press)
        shadow shouldBe 2.dp
        emit(PressInteraction.Release(press))
        shadow shouldBe 1.dp
    }

    @Test
    fun pressThenCancel() {
        composeShadow()
        val press = PressInteraction.Press(Offset.Zero)
        emit(press)
        shadow shouldBe 2.dp
        emit(PressInteraction.Cancel(press))
        shadow shouldBe 1.dp
    }

    @Test
    fun hoverThenExit() {
        composeShadow()
        val enter = HoverInteraction.Enter()
        emit(enter)
        shadow shouldBe 4.dp
        emit(HoverInteraction.Exit(enter))
        shadow shouldBe 1.dp
    }

    @Test
    fun focusThenUnfocus() {
        composeShadow()
        val focus = FocusInteraction.Focus()
        emit(focus)
        shadow shouldBe 3.dp
        emit(FocusInteraction.Unfocus(focus))
        shadow shouldBe 1.dp
    }

    @Test
    fun lastInteractionWins() {
        composeShadow()
        val press = PressInteraction.Press(Offset.Zero)
        emit(HoverInteraction.Enter())
        emit(press)
        shadow shouldBe 2.dp
        emit(PressInteraction.Release(press))
        shadow shouldBe 4.dp
    }

    @Test
    fun unknownInteractionIsIgnored() {
        composeShadow()
        emit(DragInteraction.Start())
        shadow shouldBe 1.dp
    }

    @Test
    fun disabledSnapsWithoutAnimating() {
        enabled.value = false
        composeShadow()
        shadow shouldBe 5.dp
        emit(PressInteraction.Press(Offset.Zero))
        shadow shouldBe 5.dp
    }

    @Test
    fun togglingEnabledMovesBothWays() {
        composeShadow()
        emit(PressInteraction.Press(Offset.Zero))
        shadow shouldBe 2.dp
        enabled.value = false
        compose.waitForIdle()
        shadow shouldBe 5.dp
        enabled.value = true
        compose.waitForIdle()
        shadow shouldBe 2.dp
    }

    @Test
    fun tonalElevationTracksTheSame() {
        compose.setContent {
            MaterialTheme { tonal = elevation.tonalElevation(enabled.value, source).value }
        }
        compose.waitForIdle()
        tonal shouldBe 1.dp
        val press = PressInteraction.Press(Offset.Zero)
        emit(press)
        tonal shouldBe 2.dp
        emit(PressInteraction.Release(press))
        tonal shouldBe 1.dp
    }
}
