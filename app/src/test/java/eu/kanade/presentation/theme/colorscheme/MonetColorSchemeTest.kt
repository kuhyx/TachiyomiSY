package eu.kanade.presentation.theme.colorscheme

import android.app.WallpaperColors
import android.app.WallpaperManager
import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import android.graphics.Color as AndroidColor

@RunWith(RobolectricTestRunner::class)
internal class MonetColorSchemeTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun wallpaper(colors: WallpaperColors?) {
        val manager = mockk<WallpaperManager>()
        every { manager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM) } returns colors
        mockkStatic(WallpaperManager::class)
        every { WallpaperManager.getInstance(any()) } returns manager
    }

    @Test
    fun systemDynamicColorsOnTwelve() {
        val scheme = MonetColorScheme(context)
        scheme.lightScheme shouldNotBe scheme.darkScheme
    }

    @Test
    @Config(sdk = [30])
    fun wallpaperSeedBeforeTwelve() {
        val seed = AndroidColor.valueOf(AndroidColor.RED)
        wallpaper(WallpaperColors(seed, null, null))
        val scheme = MonetColorScheme(context)
        val expected = MonetCompatColorScheme(Color(seed.toArgb()))
        scheme.lightScheme.primary shouldBe expected.lightScheme.primary
        scheme.darkScheme.primary shouldBe expected.darkScheme.primary
    }

    @Test
    @Config(sdk = [30])
    fun noWallpaperFallsBack() {
        wallpaper(null)
        MonetColorScheme(context).lightScheme shouldBe TachiyomiColorScheme.lightScheme
    }

    @Test
    @Config(sdk = [26])
    fun oreoFallsBack() {
        MonetColorScheme(context).darkScheme shouldBe TachiyomiColorScheme.darkScheme
    }
}
