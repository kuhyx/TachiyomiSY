package eu.kanade.presentation.components

import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

internal class ChipModelsTest {

    private val state = ChipStateColors(
        container = Color.Red,
        label = Color.Red,
        leadingIconContent = Color.Red,
        trailingIconContent = Color.Red,
    )
    private val other = state.copy(label = Color.Blue)
    private val absent: Any? = null

    private fun elevation(default: Int = 1, pressed: Int = 2, focused: Int = 3, hovered: Int = 4, disabled: Int = 6) =
        ChipElevation(
            defaultElevation = default.dp,
            pressedElevation = pressed.dp,
            focusedElevation = focused.dp,
            hoveredElevation = hovered.dp,
            draggedElevation = 5.dp,
            disabledElevation = disabled.dp,
        )

    @Test
    fun elevationEqualityFields() {
        val base = elevation()
        (base == base) shouldBe true
        (base == elevation()) shouldBe true
        base.equals(absent) shouldBe false
        base.equals("x") shouldBe false
        (base == elevation(default = 9)) shouldBe false
        (base == elevation(pressed = 9)) shouldBe false
        (base == elevation(focused = 9)) shouldBe false
        (base == elevation(hovered = 9)) shouldBe false
        (base == elevation(disabled = 9)) shouldBe false
        base.hashCode() shouldBe elevation().hashCode()
    }

    @Test
    fun colorsEqualityChecksBothStates() {
        val base = ChipColors(state, other)
        (base == base) shouldBe true
        (base == ChipColors(state, other)) shouldBe true
        base.equals("x") shouldBe false
        (base == ChipColors(other, other)) shouldBe false
        (base == ChipColors(state, state)) shouldBe false
        base.hashCode() shouldBe ChipColors(state, other).hashCode()
        state.toString() shouldNotBe other.toString()
    }

    @Test
    fun borderEqualityChecksEveryField() {
        val base = ChipBorder(Color.Red, Color.Blue, 1.dp)
        (base == base) shouldBe true
        (base == ChipBorder(Color.Red, Color.Blue, 1.dp)) shouldBe true
        base.equals(absent) shouldBe false
        base.equals("x") shouldBe false
        (base == ChipBorder(Color.Green, Color.Blue, 1.dp)) shouldBe false
        (base == ChipBorder(Color.Red, Color.Green, 1.dp)) shouldBe false
        (base == ChipBorder(Color.Red, Color.Blue, 2.dp)) shouldBe false
        base.hashCode() shouldBe ChipBorder(Color.Red, Color.Blue, 1.dp).hashCode()
    }

    @Test
    fun interactionsAddAndRemove() {
        val list = mutableListOf<Interaction>()
        val hover = HoverInteraction.Enter()
        val focus = FocusInteraction.Focus()
        val press = PressInteraction.Press(Offset.Zero)
        val press2 = PressInteraction.Press(Offset.Zero)
        val drag = DragInteraction.Start()
        val drag2 = DragInteraction.Start()
        listOf(hover, focus, press, press2, drag, drag2).forEach(list::apply)
        list.size shouldBe 6
        list.apply(HoverInteraction.Exit(hover))
        list.apply(FocusInteraction.Unfocus(focus))
        list.apply(PressInteraction.Release(press))
        list.apply(PressInteraction.Cancel(press2))
        list.apply(DragInteraction.Stop(drag))
        list.apply(DragInteraction.Cancel(drag2))
        list.apply(object : Interaction {})
        list.shouldBeEmpty()
    }
}
