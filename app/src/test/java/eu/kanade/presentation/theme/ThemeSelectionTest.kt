package eu.kanade.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.presentation.theme.colorscheme.BaseColorScheme
import eu.kanade.presentation.theme.colorscheme.CatppuccinColorScheme
import eu.kanade.presentation.theme.colorscheme.GreenAppleColorScheme
import eu.kanade.presentation.theme.colorscheme.LavenderColorScheme
import eu.kanade.presentation.theme.colorscheme.MidnightDuskColorScheme
import eu.kanade.presentation.theme.colorscheme.MonochromeColorScheme
import eu.kanade.presentation.theme.colorscheme.NordColorScheme
import eu.kanade.presentation.theme.colorscheme.StrawberryColorScheme
import eu.kanade.presentation.theme.colorscheme.TachiyomiColorScheme
import eu.kanade.presentation.theme.colorscheme.TakoColorScheme
import eu.kanade.presentation.theme.colorscheme.TealTurqoiseColorScheme
import eu.kanade.presentation.theme.colorscheme.TidalWaveColorScheme
import eu.kanade.presentation.theme.colorscheme.YinYangColorScheme
import eu.kanade.presentation.theme.colorscheme.YotsubaColorScheme
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Every [AppTheme] maps to its scheme; the deprecated ones fall back to the default. */
@RunWith(RobolectricTestRunner::class)
internal class ThemeSelectionTest {
    @get:Rule
    val compose = createComposeRule()

    private val expected: Map<AppTheme, BaseColorScheme> = mapOf(
        AppTheme.DEFAULT to TachiyomiColorScheme,
        AppTheme.CATPPUCCIN to CatppuccinColorScheme,
        AppTheme.GREEN_APPLE to GreenAppleColorScheme,
        AppTheme.LAVENDER to LavenderColorScheme,
        AppTheme.MIDNIGHT_DUSK to MidnightDuskColorScheme,
        AppTheme.MONOCHROME to MonochromeColorScheme,
        AppTheme.NORD to NordColorScheme,
        AppTheme.STRAWBERRY_DAIQUIRI to StrawberryColorScheme,
        AppTheme.TAKO to TakoColorScheme,
        AppTheme.TEALTURQUOISE to TealTurqoiseColorScheme,
        AppTheme.TIDAL_WAVE to TidalWaveColorScheme,
        AppTheme.YINYANG to YinYangColorScheme,
        AppTheme.YOTSUBA to YotsubaColorScheme,
        AppTheme.DARK_BLUE to TachiyomiColorScheme,
        AppTheme.HOT_PINK to TachiyomiColorScheme,
        AppTheme.BLUE to TachiyomiColorScheme,
        AppTheme.PURE_RED to TachiyomiColorScheme,
    )

    private fun collect(amoled: Boolean): Map<AppTheme, ColorScheme> {
        var theme by mutableStateOf(AppTheme.DEFAULT)
        val seen = mutableMapOf<AppTheme, ColorScheme>()
        compose.setContent {
            TachiyomiPreviewTheme(appTheme = theme, isAmoled = amoled) { seen[theme] = MaterialTheme.colorScheme }
        }
        AppTheme.entries.forEach {
            theme = it
            compose.waitForIdle()
        }
        return seen
    }

    @Test
    fun lightThemesUseTheLightScheme() {
        val seen = collect(amoled = false)
        expected.forEach { (theme, scheme) -> seen[theme]?.primary shouldBe scheme.lightScheme.primary }
        seen[AppTheme.MONET] shouldNotBe null
    }

    @Test
    @Config(qualifiers = "night")
    fun darkThemesUseTheDarkScheme() {
        val seen = collect(amoled = false)
        expected.forEach { (theme, scheme) -> seen[theme]?.primary shouldBe scheme.darkScheme.primary }
    }

    @Test
    @Config(qualifiers = "night")
    fun amoledMonetKeepsItsContainers() {
        val seen = collect(amoled = true)
        seen[AppTheme.MONET]?.background shouldBe androidx.compose.ui.graphics.Color.Black
        seen[AppTheme.NORD]?.surfaceContainer shouldNotBe NordColorScheme.darkScheme.surfaceContainer
    }
}
