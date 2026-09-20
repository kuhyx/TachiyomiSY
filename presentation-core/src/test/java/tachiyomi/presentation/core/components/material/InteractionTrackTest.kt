package tachiyomi.presentation.core.components.material

import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.geometry.Offset
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.Test

/** The list of live interactions a button keeps: starts are added, their ends remove them. */
internal class InteractionTrackTest {
    private val live = mutableListOf<Interaction>()

    @Test
    fun pressIsRemovedByRelease() {
        val press = PressInteraction.Press(Offset.Zero)
        live.track(press)
        live shouldContainExactly listOf(press)
        live.track(PressInteraction.Release(press))
        live.shouldBeEmpty()
    }

    @Test
    fun pressIsRemovedByCancel() {
        val press = PressInteraction.Press(Offset.Zero)
        live.track(press)
        live.track(PressInteraction.Cancel(press))
        live.shouldBeEmpty()
    }

    @Test
    fun hoverIsRemovedByExit() {
        val enter = HoverInteraction.Enter()
        live.track(enter)
        live shouldContainExactly listOf(enter)
        live.track(HoverInteraction.Exit(enter))
        live.shouldBeEmpty()
    }

    @Test
    fun focusIsRemovedByUnfocus() {
        val focus = FocusInteraction.Focus()
        live.track(focus)
        live shouldContainExactly listOf(focus)
        live.track(FocusInteraction.Unfocus(focus))
        live.shouldBeEmpty()
    }

    @Test
    fun unknownInteractionsAreIgnored() {
        live.track(DragInteraction.Start())
        live.track(object : Interaction {})
        live.shouldBeEmpty()
    }

    @Test
    fun keepsInsertionOrder() {
        val press = PressInteraction.Press(Offset.Zero)
        val enter = HoverInteraction.Enter()
        live.track(enter)
        live.track(press)
        live shouldContainExactly listOf(enter, press)
        live.track(HoverInteraction.Exit(enter))
        live shouldContainExactly listOf(press)
    }
}
