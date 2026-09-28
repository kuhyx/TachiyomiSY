package eu.kanade.presentation.library.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/** Draws the selection outline for real: a selected item is filled, an unselected one is left alone. */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(RobolectricTestRunner::class)
internal class SelectedOutlineDrawTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun onlySelectedIsFilled() {
        compose.setContent {
            Row {
                Box(Modifier.size(10.dp).testTag("on").selectedOutline(isSelected = true, color = Color.Red))
                Box(Modifier.size(10.dp).testTag("off").selectedOutline(isSelected = false, color = Color.Red))
            }
        }
        compose.onNodeWithTag("on").captureToImage().toPixelMap()[1, 1] shouldBe Color.Red
        compose.onNodeWithTag("off").captureToImage().toPixelMap()[1, 1] shouldNotBe Color.Red
    }
}
