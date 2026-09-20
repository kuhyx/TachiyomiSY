package tachiyomi.presentation.core.components.material

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ButtonDefaultsTest {
    @get:Rule
    val compose = createComposeRule()

    private var scheme: ColorScheme = lightColorScheme()
    private var colors: ButtonColors? = null

    @Test
    fun buttonColorsTakeTheTheme() {
        compose.setContent {
            MaterialTheme {
                scheme = MaterialTheme.colorScheme
                colors = ButtonDefaults.buttonColors()
            }
        }
        compose.waitForIdle()
        colors shouldBe ButtonColors(
            containerColor = scheme.primary,
            contentColor = scheme.onPrimary,
            disabledContainerColor = scheme.onSurface.copy(alpha = 0.12f),
            disabledContentColor = scheme.onSurface.copy(alpha = DISABLED_ALPHA),
        )
    }

    @Test
    fun buttonColorsKeepGivenColours() {
        compose.setContent {
            MaterialTheme {
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red,
                    contentColor = Color.White,
                    disabledContainerColor = Color.Gray,
                    disabledContentColor = Color.Black,
                )
            }
        }
        compose.waitForIdle()
        colors shouldBe ButtonColors(
            containerColor = Color.Red,
            contentColor = Color.White,
            disabledContainerColor = Color.Gray,
            disabledContentColor = Color.Black,
        )
    }

    @Test
    fun textButtonColorsAreTransparent() {
        compose.setContent {
            MaterialTheme {
                scheme = MaterialTheme.colorScheme
                colors = ButtonDefaults.textButtonColors()
            }
        }
        compose.waitForIdle()
        colors shouldBe ButtonColors(
            containerColor = Color.Transparent,
            contentColor = scheme.primary,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = scheme.onSurface.copy(alpha = DISABLED_ALPHA),
        )
    }

    @Test
    fun buttonElevationDefaults() {
        ButtonDefaults.buttonElevation() shouldBe ButtonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 1.dp,
            disabledElevation = 0.dp,
        )
    }

    @Test
    fun buttonElevationKeepsValues() {
        val elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 1.dp,
            pressedElevation = 2.dp,
            focusedElevation = 3.dp,
            hoveredElevation = 4.dp,
            disabledElevation = 5.dp,
        )
        elevation shouldBe ButtonElevation(
            defaultElevation = 1.dp,
            pressedElevation = 2.dp,
            focusedElevation = 3.dp,
            hoveredElevation = 4.dp,
            disabledElevation = 5.dp,
        )
    }

    @Test
    fun flatElevationIsAllZero() {
        ButtonDefaults.flatElevation() shouldBe ButtonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp,
            disabledElevation = 0.dp,
        )
    }
}
