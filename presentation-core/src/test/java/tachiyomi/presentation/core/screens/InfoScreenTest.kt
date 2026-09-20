package tachiyomi.presentation.core.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
internal class InfoScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private var accepted = 0
    private var rejected = 0

    private fun drawRootViewManually() {
        val root = compose.onRoot().fetchSemanticsNode().root as ViewRootForTest
        compose.runOnUiThread {
            val view = root.view
            view.width shouldNotBe 0
            view.height shouldNotBe 0
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            bitmap.recycle()
        }
    }

    @Test
    fun acceptOnlyByDefault() {
        compose.setContent {
            MaterialTheme {
                InfoScreen(
                    icon = Icons.Default.Info,
                    headingText = "Heading",
                    subtitleText = "Subtitle",
                    acceptText = "Accept",
                    onAcceptClick = { accepted++ },
                ) {
                    Text("Body")
                }
            }
        }
        compose.onNodeWithText("Heading").assertIsDisplayed()
        compose.onNodeWithText("Subtitle").assertIsDisplayed()
        compose.onNodeWithText("Body").assertIsDisplayed()
        compose.onNodeWithText("Reject").assertDoesNotExist()
        compose.onNodeWithText("Accept").assertIsEnabled().performClick()
        compose.runOnIdle { accepted shouldBe 1 }
    }

    @Test
    fun rejectButtonWhenBothGiven() {
        compose.setContent {
            MaterialTheme {
                InfoScreen(
                    icon = Icons.Default.Info,
                    headingText = "Heading",
                    subtitleText = "Subtitle",
                    acceptText = "Accept",
                    onAcceptClick = { accepted++ },
                    canAccept = true,
                    rejectText = "Reject",
                    onRejectClick = { rejected++ },
                ) {
                    Text("Body")
                }
            }
        }
        compose.onNodeWithText("Reject").performClick()
        compose.runOnIdle {
            rejected shouldBe 1
            accepted shouldBe 0
        }
    }

    @Test
    fun rejectTextAloneIsHidden() {
        compose.setContent {
            MaterialTheme {
                InfoScreen(
                    icon = Icons.Default.Info,
                    headingText = "Heading",
                    subtitleText = "Subtitle",
                    acceptText = "Accept",
                    onAcceptClick = { accepted++ },
                    canAccept = false,
                    rejectText = "Reject",
                    onRejectClick = null,
                ) {
                    Text("Body")
                }
            }
        }
        compose.onNodeWithText("Reject").assertDoesNotExist()
        compose.onNodeWithText("Accept").assertIsNotEnabled()
    }

    @Test
    fun rejectHandlerAloneIsHidden() {
        compose.setContent {
            MaterialTheme {
                InfoScreen(
                    icon = Icons.Default.Info,
                    headingText = "Heading",
                    subtitleText = "Subtitle",
                    acceptText = "Accept",
                    onAcceptClick = { accepted++ },
                    rejectText = null,
                    onRejectClick = { rejected++ },
                ) {
                    Text("Body")
                }
            }
        }
        compose.onNodeWithText("Reject").assertDoesNotExist()
        compose.onNodeWithText("Accept").assertIsEnabled()
    }

    @Test
    fun previewShowsBothButtons() {
        compose.setContent { MaterialTheme { InfoScaffoldPreview() } }
        compose.onNodeWithText("Hello world").assertIsDisplayed()
        compose.onNodeWithText("Accept").assertIsDisplayed()
        compose.onNodeWithText("Reject").assertIsDisplayed()
    }

    @Test
    fun bottomBarDrawsItsTopBorder() {
        compose.setContent { MaterialTheme { InfoScaffoldPreview() } }
        drawRootViewManually()
        compose.onNodeWithText("Accept").assertIsDisplayed()
    }
}
