package tachiyomi.presentation.core.components.material

import androidx.compose.ui.graphics.Color
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

internal class ButtonColorsEqualityTest {
    private val colors = ButtonColors(
        containerColor = Color.Red,
        contentColor = Color.White,
        disabledContainerColor = Color.Gray,
        disabledContentColor = Color.Black,
    )

    @Test
    fun sameInstance() {
        colors.equals(colors) shouldBe true
    }

    @Test
    fun sameValues() {
        val other = ButtonColors(
            containerColor = Color.Red,
            contentColor = Color.White,
            disabledContainerColor = Color.Gray,
            disabledContentColor = Color.Black,
        )
        colors shouldBe other
        colors.hashCode() shouldBe other.hashCode()
    }

    @Test
    fun otherTypeAndNull() {
        val nothing: Any? = null
        colors.equals(nothing) shouldBe false
        colors.equals("colors") shouldBe false
    }

    @Test
    fun differentContainer() {
        colors shouldNotBe ButtonColors(
            containerColor = Color.Blue,
            contentColor = Color.White,
            disabledContainerColor = Color.Gray,
            disabledContentColor = Color.Black,
        )
    }

    @Test
    fun differentContent() {
        colors shouldNotBe ButtonColors(
            containerColor = Color.Red,
            contentColor = Color.Blue,
            disabledContainerColor = Color.Gray,
            disabledContentColor = Color.Black,
        )
    }

    @Test
    fun differentDisabledContainer() {
        colors shouldNotBe ButtonColors(
            containerColor = Color.Red,
            contentColor = Color.White,
            disabledContainerColor = Color.Blue,
            disabledContentColor = Color.Black,
        )
    }

    @Test
    fun differentDisabledContent() {
        val other = ButtonColors(
            containerColor = Color.Red,
            contentColor = Color.White,
            disabledContainerColor = Color.Gray,
            disabledContentColor = Color.Blue,
        )
        colors shouldNotBe other
        colors.hashCode() shouldNotBe other.hashCode()
    }
}
