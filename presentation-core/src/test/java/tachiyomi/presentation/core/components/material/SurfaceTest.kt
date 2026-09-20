package tachiyomi.presentation.core.components.material

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalAbsoluteTonalElevation
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class SurfaceTest {
    @get:Rule
    val compose = createComposeRule()

    private var clicks = 0
    private var longClicks = 0
    private var contentColor = Color.Unspecified
    private var onSurface = Color.Unspecified
    private var absoluteElevation: Dp = Dp.Unspecified

    @Test
    fun defaultsAreTheSurfaceColours() {
        compose.setContent {
            MaterialTheme {
                onSurface = MaterialTheme.colorScheme.onSurface
                Surface(onClick = { clicks += 1 }) {
                    Text("plain")
                    contentColor = LocalContentColor.current
                    absoluteElevation = LocalAbsoluteTonalElevation.current
                }
            }
        }
        compose.onNodeWithText("plain").assertIsDisplayed().performClick()
        clicks shouldBe 1
        contentColor shouldBe onSurface
        absoluteElevation shouldBe 0.dp
    }

    @Test
    fun everyParameterGiven() {
        compose.setContent {
            MaterialTheme {
                Surface(
                    onClick = { clicks += 1 },
                    modifier = Modifier.testTag("surface"),
                    onLongClick = { longClicks += 1 },
                    enabled = true,
                    shape = CircleShape,
                    color = Color.Red,
                    contentColor = Color.White,
                    tonalElevation = 2.dp,
                    shadowElevation = 3.dp,
                    border = BorderStroke(1.dp, Color.Black),
                    interactionSource = remember { MutableInteractionSource() },
                ) {
                    Text("full")
                    contentColor = LocalContentColor.current
                    absoluteElevation = LocalAbsoluteTonalElevation.current
                }
            }
        }
        compose.onNodeWithTag("surface").assertIsDisplayed().performTouchInput { longClick() }
        longClicks shouldBe 1
        clicks shouldBe 0
        compose.onNodeWithTag("surface").performClick()
        clicks shouldBe 1
        contentColor shouldBe Color.White
        absoluteElevation shouldBe 2.dp
    }

    @Test
    fun tonalElevationTintsTheSurface() {
        compose.setContent {
            MaterialTheme {
                Surface(onClick = { clicks += 1 }, tonalElevation = 4.dp) {
                    Text("tinted")
                    absoluteElevation = LocalAbsoluteTonalElevation.current
                }
            }
        }
        compose.onNodeWithText("tinted").assertIsDisplayed()
        absoluteElevation shouldBe 4.dp
    }

    @Test
    fun disabledIgnoresClicks() {
        compose.setContent {
            MaterialTheme {
                Surface(onClick = { clicks += 1 }, onLongClick = { longClicks += 1 }, enabled = false) {
                    Text("off")
                }
            }
        }
        compose.onNodeWithText("off").assertIsNotEnabled().performClick()
        compose.onNodeWithText("off").performTouchInput { longClick() }
        clicks shouldBe 0
        longClicks shouldBe 0
    }
}
