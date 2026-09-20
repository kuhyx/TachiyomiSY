package tachiyomi.presentation.core.util

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

private const val TAG = "target"

/** Pixel-level checks need real drawing, hence native graphics. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
internal class ModifierTest {
    @get:Rule
    val compose = createComposeRule()

    private var clicks = 0
    private var longClicks = 0
    private var enters = 0

    private fun setBox(modifier: Modifier) {
        compose.setContent {
            MaterialTheme {
                Box(modifier = Modifier.size(20.dp).background(Color.White).then(modifier).testTag(TAG))
            }
        }
    }

    private fun centrePixel(): Color = compose.onNodeWithTag(TAG).captureToImage().toPixelMap()[10, 10]

    @Test
    fun selectedBackgroundTints() {
        compose.setContent {
            MaterialTheme {
                Box(modifier = Modifier.size(20.dp).background(Color.White).selectedBackground(true).testTag(TAG))
            }
        }
        centrePixel() shouldNotBe Color.White
    }

    @Test
    fun unselectedBackgroundIsPlain() {
        compose.setContent {
            MaterialTheme {
                Box(modifier = Modifier.size(20.dp).background(Color.White).selectedBackground(false).testTag(TAG))
            }
        }
        centrePixel() shouldBe Color.White
    }

    @Test
    fun secondaryItemAlphaDimsContent() {
        setBox(Modifier.secondaryItemAlpha().background(Color.Black))
        val pixel = centrePixel()
        pixel shouldNotBe Color.Black
        pixel shouldNotBe Color.White
    }

    @Test
    fun clickableNoIndicationClicks() {
        setBox(Modifier.clickableNoIndication { clicks++ })
        compose.onNodeWithTag(TAG).performClick()
        compose.waitForIdle()
        clicks shouldBe 1
        // With no long-click handler a long press falls through to onClick on release.
        compose.onNodeWithTag(TAG).performTouchInput { longClick() }
        compose.waitForIdle()
        clicks shouldBe 2
        longClicks shouldBe 0
    }

    @Test
    fun clickableNoIndicationLongClick() {
        setBox(Modifier.clickableNoIndication(onLongClick = { longClicks++ }, onClick = { clicks++ }))
        compose.onNodeWithTag(TAG).performTouchInput { longClick() }
        compose.onNodeWithTag(TAG).performClick()
        compose.waitForIdle()
        longClicks shouldBe 1
        clicks shouldBe 1
    }

    @Test
    fun enterKeyDownRunsAction() {
        setBox(Modifier.runOnEnterKeyPressed { enters++ }.focusable())
        compose.onNodeWithTag(TAG).requestFocus()
        compose.onNodeWithTag(TAG).assertIsFocused()
        compose.onNodeWithTag(TAG).performKeyInput { pressKey(Key.Enter) }
        enters shouldBe 1
        compose.onNodeWithTag(TAG).performKeyInput { pressKey(Key.NumPadEnter) }
        enters shouldBe 2
        compose.onNodeWithTag(TAG).performKeyInput { pressKey(Key.A) }
        enters shouldBe 2
    }
}
