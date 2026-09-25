package eu.kanade.presentation.theme.colorscheme

import androidx.compose.ui.graphics.Color
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class BaseColorSchemeTest {

    private fun scheme(dark: Boolean, amoled: Boolean, override: Boolean) = NordColorScheme.getColorScheme(
        isDark = dark,
        isAmoled = amoled,
        overrideDarkSurfaceContainers = override,
    )

    @Test
    fun lightIgnoresTheAmoledFlag() {
        scheme(dark = false, amoled = true, override = true) shouldBe NordColorScheme.lightScheme
    }

    @Test
    fun darkWithoutAmoledIsPlain() {
        scheme(dark = true, amoled = false, override = true) shouldBe NordColorScheme.darkScheme
    }

    @Test
    fun amoledBlackensTheSurface() {
        val amoled = scheme(dark = true, amoled = true, override = false)
        amoled.background shouldBe Color.Black
        amoled.surface shouldBe Color.Black
        amoled.onSurface shouldBe Color.White
        amoled.surfaceContainer shouldBe NordColorScheme.darkScheme.surfaceContainer
    }

    @Test
    fun amoledOverridesContainers() {
        val amoled = scheme(dark = true, amoled = true, override = true)
        amoled.surfaceContainer shouldBe Color(color = 0xFF0C0C0C)
        amoled.surfaceContainerHigh shouldBe Color(color = 0xFF131313)
        amoled.surfaceContainerHighest shouldBe Color(color = 0xFF1B1B1B)
    }
}
