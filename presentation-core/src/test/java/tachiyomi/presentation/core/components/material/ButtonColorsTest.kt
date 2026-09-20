package tachiyomi.presentation.core.components.material

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ButtonColorsTest {
    @get:Rule
    val compose = createComposeRule()

    private val colors = ButtonColors(
        containerColor = Color.Red,
        contentColor = Color.White,
        disabledContainerColor = Color.Gray,
        disabledContentColor = Color.Black,
    )
    private var enabledContainer = Color.Unspecified
    private var disabledContainer = Color.Unspecified
    private var enabledContent = Color.Unspecified
    private var disabledContent = Color.Unspecified

    @Test
    fun containerColorFollowsEnabled() {
        compose.setContent {
            MaterialTheme {
                enabledContainer = colors.containerColor(true).value
                disabledContainer = colors.containerColor(false).value
            }
        }
        compose.waitForIdle()
        enabledContainer shouldBe Color.Red
        disabledContainer shouldBe Color.Gray
    }

    @Test
    fun contentColorFollowsEnabled() {
        compose.setContent {
            MaterialTheme {
                enabledContent = colors.contentColor(true).value
                disabledContent = colors.contentColor(false).value
            }
        }
        compose.waitForIdle()
        enabledContent shouldBe Color.White
        disabledContent shouldBe Color.Black
    }
}
