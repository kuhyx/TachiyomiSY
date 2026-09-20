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
import androidx.compose.ui.test.assertIsNotEnabled
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
internal class ButtonTest {
    @get:Rule
    val compose = createComposeRule()

    private var clicks = 0
    private var longClicks = 0
    private var contentColor = Color.Unspecified
    private var onPrimary = Color.Unspecified

    @Test
    fun defaultsClickAndRender() {
        compose.setContent {
            MaterialTheme {
                onPrimary = MaterialTheme.colorScheme.onPrimary
                Button(onClick = { clicks += 1 }) {
                    Text("plain")
                    contentColor = LocalContentColor.current
                }
            }
        }
        compose.onNodeWithText("plain").assertIsDisplayed().performClick()
        clicks shouldBe 1
        contentColor shouldBe onPrimary
    }

    @Test
    fun everyParameterGiven() {
        compose.setContent {
            MaterialTheme {
                Button(
                    onClick = { clicks += 1 },
                    modifier = Modifier.testTag("button"),
                    onLongClick = { longClicks += 1 },
                    enabled = true,
                    interactionSource = remember { MutableInteractionSource() },
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, Color.Black),
                    colors = ButtonDefaults.buttonColors(contentColor = Color.Red),
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text("full")
                    contentColor = LocalContentColor.current
                }
            }
        }
        compose.onNodeWithTag("button").assertIsDisplayed().performTouchInput { longClick() }
        longClicks shouldBe 1
        clicks shouldBe 0
        compose.onNodeWithTag("button").performClick()
        clicks shouldBe 1
        contentColor shouldBe Color.Red
    }

    @Test
    fun disabledUsesDisabledColours() {
        compose.setContent {
            MaterialTheme {
                Button(
                    onClick = { clicks += 1 },
                    modifier = Modifier.testTag("off"),
                    enabled = false,
                    colors = ButtonDefaults.buttonColors(contentColor = Color.Red, disabledContentColor = Color.Blue),
                ) {
                    Text("off")
                    contentColor = LocalContentColor.current
                }
            }
        }
        compose.onNodeWithTag("off").assertIsNotEnabled().performClick()
        clicks shouldBe 0
        contentColor shouldBe Color.Blue
    }
}
