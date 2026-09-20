package tachiyomi.presentation.core.util

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
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
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val TAG = "target"

/** [selectedBackground] picks the dimmer tint when the system is in night mode. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "night")
internal class ModifierSelectedNightTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun selectedUsesNightTint() {
        var dark = false
        compose.setContent {
            dark = isSystemInDarkTheme()
            MaterialTheme {
                Box(modifier = Modifier.size(20.dp).background(Color.White).selectedBackground(true).testTag(TAG))
            }
        }
        dark shouldBe true
        compose.onNodeWithTag(TAG).captureToImage().toPixelMap()[10, 10] shouldNotBe Color.White
    }
}
