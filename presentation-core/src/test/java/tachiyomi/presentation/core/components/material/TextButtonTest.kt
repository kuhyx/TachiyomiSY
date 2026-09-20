package tachiyomi.presentation.core.components.material

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class TextButtonTest {
    @get:Rule
    val compose = createComposeRule()

    private var clicks = 0
    private var longClicks = 0
    private var contentColor = Color.Unspecified
    private var primary = Color.Unspecified
    private var onSurface = Color.Unspecified

    @Test
    fun defaultsUseThePrimaryColour() {
        compose.setContent {
            MaterialTheme {
                primary = MaterialTheme.colorScheme.primary
                TextButton(onClick = { clicks += 1 }) {
                    Text("text")
                    contentColor = LocalContentColor.current
                }
            }
        }
        compose.onNodeWithText("text").assertIsDisplayed().performClick()
        clicks shouldBe 1
        contentColor shouldBe primary
    }

    @Test
    fun everyParameterGiven() {
        compose.setContent {
            MaterialTheme {
                TextButton(
                    onClick = { clicks += 1 },
                    modifier = Modifier.testTag("text"),
                    onLongClick = { longClicks += 1 },
                    enabled = true,
                    interactionSource = remember { MutableInteractionSource() },
                    elevation = ButtonDefaults.buttonElevation(),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, Color.Black),
                    colors = ButtonDefaults.buttonColors(contentColor = Color.Green),
                    contentPadding = PaddingValues(2.dp),
                ) {
                    Text("full")
                    contentColor = LocalContentColor.current
                }
            }
        }
        compose.onNodeWithTag("text").performTouchInput { longClick() }
        longClicks shouldBe 1
        contentColor shouldBe Color.Green
    }

    @Test
    fun disabledUsesDisabledColour() {
        compose.setContent {
            MaterialTheme {
                onSurface = MaterialTheme.colorScheme.onSurface
                TextButton(onClick = { clicks += 1 }, enabled = false) {
                    Text("off")
                    contentColor = LocalContentColor.current
                }
            }
        }
        compose.onNodeWithText("off").performClick()
        clicks shouldBe 0
        contentColor shouldBe onSurface.copy(alpha = DISABLED_ALPHA)
    }
}
