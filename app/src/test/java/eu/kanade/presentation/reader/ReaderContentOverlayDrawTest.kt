package eu.kanade.presentation.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import eu.kanade.domain.FlowPreferenceStore
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

private const val FLASH_FRAMES = 5

/** Draws the overlays into a bitmap: their Canvas lambdas only run when a frame is actually drawn. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
internal class ReaderContentOverlayDrawTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun overlaysDraw() {
        compose.setContent {
            Box(Modifier.size(20.dp)) {
                ReaderContentOverlay(brightness = -50, color = 0x55FF0000, colorBlendMode = BlendMode.Multiply)
                ReaderContentOverlay(brightness = 10, color = 0x55FF0000, colorBlendMode = null)
            }
        }
        val image = compose.onRoot().captureToImage()
        (image.width > 0) shouldBe true
    }

    @Test
    fun aFlashDraws() {
        val preferences = ReaderPreferences(FlowPreferenceStore())
        preferences.flashDurationMillis.set(1_000)
        startKoin { modules(module { single { preferences } }) }
        try {
            val host = DisplayRefreshHost()
            compose.setContent { Box(Modifier.size(20.dp)) { DisplayRefreshHost(hostState = host) } }
            compose.waitForIdle()
            // Before any flash the canvas has nothing to paint.
            compose.onRoot().captureToImage()
            compose.mainClock.autoAdvance = false
            compose.runOnIdle { host.flash() }
            // A few frames in, the effect has picked the flash colour and the canvas paints it.
            repeat(FLASH_FRAMES) {
                compose.mainClock.advanceTimeByFrame()
                compose.waitForIdle()
            }
            val pixels = compose.onRoot().captureToImage().toPixelMap()
            pixels[pixels.width / 2, pixels.height / 2] shouldBe Color.Black
        } finally {
            stopKoin()
        }
    }
}
