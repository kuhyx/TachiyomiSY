package tachiyomi.presentation.core.util

import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.geometry.Offset
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/** `to != null`: the incoming spec of the new state decides, whatever `from` is. */
internal class ElevationIncomingTest {
    private val unknown = object : Interaction {}

    @Test
    fun pressAnimates() = runTest {
        animateFrames(from = null, to = PressInteraction.Press(Offset.Zero)) shouldBeGreaterThan 0
    }

    @Test
    fun dragAnimates() = runTest {
        animateFrames(from = null, to = DragInteraction.Start()) shouldBeGreaterThan 0
    }

    @Test
    fun hoverAnimates() = runTest {
        animateFrames(from = null, to = HoverInteraction.Enter()) shouldBeGreaterThan 0
    }

    @Test
    fun focusAnimates() = runTest {
        animateFrames(from = null, to = FocusInteraction.Focus()) shouldBeGreaterThan 0
    }

    @Test
    fun unknownSnaps() = runTest {
        animateFrames(from = null, to = unknown) shouldBe 0
    }

    @Test
    fun toWinsOverFrom() = runTest {
        animateFrames(from = HoverInteraction.Enter(), to = PressInteraction.Press(Offset.Zero)) shouldBeGreaterThan 0
        animateFrames(from = PressInteraction.Press(Offset.Zero), to = unknown) shouldBe 0
    }
}
