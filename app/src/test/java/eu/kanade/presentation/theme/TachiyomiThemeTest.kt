package eu.kanade.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.presentation.theme.colorscheme.NordColorScheme
import eu.kanade.presentation.theme.colorscheme.TachiyomiColorScheme
import eu.kanade.presentation.theme.colorscheme.TakoColorScheme
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import eu.kanade.domain.FlowPreferenceStore

@RunWith(RobolectricTestRunner::class)
internal class TachiyomiThemeTest {
    @get:Rule
    val compose = createComposeRule()

    private val preferences = UiPreferences(FlowPreferenceStore())
    private var seen: ColorScheme? = null

    @Before
    fun setUp() {
        startKoin { modules(module { single { preferences } }) }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun themeReadsThePreferences() {
        preferences.appTheme.set(AppTheme.NORD)
        compose.setContent { TachiyomiTheme { seen = MaterialTheme.colorScheme } }
        compose.waitForIdle()
        seen?.primary shouldBe NordColorScheme.lightScheme.primary
    }

    @Test
    fun argumentsOverridePreferences() {
        preferences.appTheme.set(AppTheme.NORD)
        compose.setContent {
            TachiyomiTheme(appTheme = AppTheme.TAKO, amoled = true) { seen = MaterialTheme.colorScheme }
        }
        compose.waitForIdle()
        seen?.primary shouldBe TakoColorScheme.lightScheme.primary
    }

    @Test
    fun previewDefaultsToTachiyomi() {
        compose.setContent { TachiyomiPreviewTheme { seen = MaterialTheme.colorScheme } }
        compose.waitForIdle()
        seen?.primary shouldBe TachiyomiColorScheme.lightScheme.primary
    }

    @Test
    @Config(qualifiers = "night")
    fun darkAmoledPreview() {
        compose.setContent {
            TachiyomiPreviewTheme(appTheme = AppTheme.TAKO, isAmoled = true) { seen = MaterialTheme.colorScheme }
        }
        compose.waitForIdle()
        seen?.primary shouldBe TakoColorScheme.darkScheme.primary
        seen?.surfaceContainer shouldBe TakoColorScheme.getColorScheme(
            isDark = true,
            isAmoled = true,
            overrideDarkSurfaceContainers = true,
        ).surfaceContainer
    }
}
