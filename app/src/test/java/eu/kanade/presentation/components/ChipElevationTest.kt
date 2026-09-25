package eu.kanade.presentation.components

import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ChipElevationTest {
    @get:Rule
    val compose = createComposeRule()

    private val source = MutableInteractionSource()
    private var enabled by mutableStateOf(true)
    private var tonal: Dp = Dp.Unspecified
    private var shadow: Dp = Dp.Unspecified

    private val elevation = ChipElevation(
        defaultElevation = 1.dp,
        pressedElevation = 2.dp,
        focusedElevation = 3.dp,
        hoveredElevation = 4.dp,
        draggedElevation = 5.dp,
        disabledElevation = 6.dp,
    )

    private fun show() {
        compose.setContent {
            tonal = elevation.tonalElevation(enabled, source).value
            shadow = elevation.shadowElevation(enabled, source).value
        }
        compose.waitForIdle()
    }

    private fun emit(interaction: Interaction) {
        runBlocking { source.emit(interaction) }
        compose.mainClock.advanceTimeBy(1_000L)
        compose.waitForIdle()
    }

    @Test
    fun everyInteractionHasItsElevation() {
        show()
        tonal shouldBe 1.dp
        val press = PressInteraction.Press(Offset.Zero)
        emit(press)
        tonal shouldBe 2.dp
        emit(PressInteraction.Release(press))
        tonal shouldBe 1.dp
        val hover = HoverInteraction.Enter()
        emit(hover)
        tonal shouldBe 4.dp
        emit(HoverInteraction.Exit(hover))
        val focus = FocusInteraction.Focus()
        emit(focus)
        tonal shouldBe 3.dp
        emit(FocusInteraction.Unfocus(focus))
        val drag = DragInteraction.Start()
        emit(drag)
        shadow shouldBe 5.dp
        emit(DragInteraction.Stop(drag))
        shadow shouldBe 1.dp
    }

    @Test
    fun disabledSnapsToItsElevation() {
        show()
        enabled = false
        compose.waitForIdle()
        tonal shouldBe 6.dp
    }
}
