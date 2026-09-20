package tachiyomi.presentation.core.util

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Test

/** `to == null`: the outgoing spec of the previous state decides; no state at all snaps. */
internal class ElevationOutgoingTest {
    private val unknown = object : Interaction {}

    @Test
    fun pressAnimates() = runTest {
        animateFrames(from = PressInteraction.Press(Offset.Zero), to = null) shouldBeGreaterThan 0
    }

    @Test
    fun dragAnimates() = runTest {
        animateFrames(from = DragInteraction.Start(), to = null) shouldBeGreaterThan 0
    }

    @Test
    fun hoverAnimatesFasterThanPress() = runTest {
        val hover = animateFrames(from = HoverInteraction.Enter(), to = null)
        hover shouldBeGreaterThan 0
        hover shouldBeLessThan animateFrames(from = PressInteraction.Press(Offset.Zero), to = null)
    }

    @Test
    fun focusAnimates() = runTest {
        animateFrames(from = FocusInteraction.Focus(), to = null) shouldBeGreaterThan 0
    }

    @Test
    fun unknownSnaps() = runTest {
        animateFrames(from = unknown, to = null) shouldBe 0
    }

    @Test
    fun nothingSnaps() = runTest {
        animateFrames(from = null, to = null) shouldBe 0
    }

    @Test
    fun defaultsSnap() = runTest {
        val clock = ImmediateFrameClock()
        val animatable = Animatable(0.dp, Dp.VectorConverter)
        withContext(clock) { animatable.animateElevation(target = 2.dp) }
        animatable.value shouldBe 2.dp
        clock.frames shouldBe 0
    }

    @Test
    fun onlyFromGiven() = runTest {
        val clock = ImmediateFrameClock()
        val animatable = Animatable(0.dp, Dp.VectorConverter)
        withContext(clock) { animatable.animateElevation(target = 2.dp, from = FocusInteraction.Focus()) }
        animatable.value shouldBe 2.dp
        clock.frames shouldBeGreaterThan 0
    }

    @Test
    fun onlyToGiven() = runTest {
        val clock = ImmediateFrameClock()
        val animatable = Animatable(0.dp, Dp.VectorConverter)
        withContext(clock) { animatable.animateElevation(target = 2.dp, to = FocusInteraction.Focus()) }
        animatable.value shouldBe 2.dp
        clock.frames shouldBeGreaterThan 0
    }
}
