package eu.kanade.tachiyomi.util.system

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.ThemeMode
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.preference.InMemoryPreferenceStore

/** The reader wraps its context so the background follows the reader theme rather than the app theme. */
@RunWith(RobolectricTestRunner::class)
internal class ContextReaderThemeTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val uiPreferences = UiPreferences(InMemoryPreferenceStore())
    private val readerPreferences = ReaderPreferences(InMemoryPreferenceStore())

    @Before
    fun setUp() {
        startKoin {
            modules(
                module {
                    single { uiPreferences }
                    single { readerPreferences }
                },
            )
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun aLightReaderKeepsTheDayContext() {
        context.isNightMode() shouldBe false
        readerPreferences.readerTheme.set(0)
        context.createReaderThemeContext() shouldBeSameInstanceAs context
    }

    @Test
    fun aDarkReaderWrapsTheContext() {
        readerPreferences.readerTheme.set(1)
        val wrapped = context.createReaderThemeContext()
        wrapped shouldNotBeSameInstanceAs context
        val night = wrapped.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        night shouldBe Configuration.UI_MODE_NIGHT_YES
        readerPreferences.readerTheme.set(2)
        context.createReaderThemeContext() shouldNotBeSameInstanceAs context
    }

    @Test
    fun anAutomaticReaderFollowsTheApp() {
        readerPreferences.readerTheme.set(3)
        uiPreferences.themeMode.set(ThemeMode.SYSTEM)
        context.createReaderThemeContext() shouldBeSameInstanceAs context
        uiPreferences.themeMode.set(ThemeMode.DARK)
        context.createReaderThemeContext() shouldNotBeSameInstanceAs context
        uiPreferences.themeMode.set(ThemeMode.LIGHT)
        context.createReaderThemeContext() shouldBeSameInstanceAs context
    }
}
