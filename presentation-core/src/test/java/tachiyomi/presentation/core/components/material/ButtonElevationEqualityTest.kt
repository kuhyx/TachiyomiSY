package tachiyomi.presentation.core.components.material

import androidx.compose.ui.unit.dp
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

internal class ButtonElevationEqualityTest {
    private val elevation = ButtonElevation(
        defaultElevation = 1.dp,
        pressedElevation = 2.dp,
        focusedElevation = 3.dp,
        hoveredElevation = 4.dp,
        disabledElevation = 5.dp,
    )

    @Test
    fun sameInstance() {
        elevation.equals(elevation) shouldBe true
    }

    @Test
    fun sameValues() {
        val other = ButtonElevation(
            defaultElevation = 1.dp,
            pressedElevation = 2.dp,
            focusedElevation = 3.dp,
            hoveredElevation = 4.dp,
            disabledElevation = 5.dp,
        )
        elevation shouldBe other
        elevation.hashCode() shouldBe other.hashCode()
    }

    @Test
    fun otherTypeAndNull() {
        val nothing: Any? = null
        elevation.equals(nothing) shouldBe false
        elevation.equals(1.dp) shouldBe false
    }

    @Test
    fun differentDefault() {
        elevation shouldNotBe ButtonElevation(
            defaultElevation = 9.dp,
            pressedElevation = 2.dp,
            focusedElevation = 3.dp,
            hoveredElevation = 4.dp,
            disabledElevation = 5.dp,
        )
    }

    @Test
    fun differentPressed() {
        elevation shouldNotBe ButtonElevation(
            defaultElevation = 1.dp,
            pressedElevation = 9.dp,
            focusedElevation = 3.dp,
            hoveredElevation = 4.dp,
            disabledElevation = 5.dp,
        )
    }

    @Test
    fun differentFocused() {
        elevation shouldNotBe ButtonElevation(
            defaultElevation = 1.dp,
            pressedElevation = 2.dp,
            focusedElevation = 9.dp,
            hoveredElevation = 4.dp,
            disabledElevation = 5.dp,
        )
    }

    @Test
    fun differentHovered() {
        elevation shouldNotBe ButtonElevation(
            defaultElevation = 1.dp,
            pressedElevation = 2.dp,
            focusedElevation = 3.dp,
            hoveredElevation = 9.dp,
            disabledElevation = 5.dp,
        )
    }

    @Test
    fun differentDisabled() {
        val other = ButtonElevation(
            defaultElevation = 1.dp,
            pressedElevation = 2.dp,
            focusedElevation = 3.dp,
            hoveredElevation = 4.dp,
            disabledElevation = 9.dp,
        )
        elevation shouldNotBe other
        elevation.hashCode() shouldNotBe other.hashCode()
    }
}
