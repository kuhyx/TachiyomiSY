package eu.kanade.presentation.more.settings.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.domain.ui.model.TabletUiMode
import eu.kanade.domain.ui.model.ThemeMode
import eu.kanade.presentation.more.settings.screen.appearance.AppLanguageScreen
import io.kotest.matchers.shouldBe
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class SettingsAppearanceScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = SettingsKoin()
    private val harness = SettingsHarness(compose)

    @Before
    fun setUp() {
        koin.start()
    }

    @After
    fun tearDown() {
        koin.stop()
    }

    @Test
    fun themeWidgetsStoreChoice() {
        harness.show(SettingsAppearanceScreen)
        compose.onNodeWithText("Light").performClick()
        compose.waitForIdle()
        koin.ui.themeMode.get() shouldBe ThemeMode.LIGHT
        harness.item("Pure black dark mode").enabled shouldBe false
        compose.onAllNodesWithContentDescription("Selected").onFirst().performClick()
        compose.waitForIdle()
        koin.ui.appTheme.get() shouldBe AppTheme.DEFAULT
    }

    @Test
    fun amoledOutsideActivity() {
        koin.ui.themeMode.set(ThemeMode.DARK)
        harness.show(SettingsAppearanceScreen)
        harness.item("Pure black dark mode").enabled shouldBe true
        harness.switch("Pure black dark mode", value = true) shouldBe true
    }

    @Test
    fun amoledRecreatesActivity() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        harness.show(SettingsAppearanceScreen, context = activity)
        harness.switch("Pure black dark mode", value = true) shouldBe true
    }

    @Test
    fun displayGroupCallbacks() {
        harness.show(SettingsAppearanceScreen)
        harness.click("App language")
        verify { harness.navigator.push(any<AppLanguageScreen>()) }
        harness.list("Tablet UI", TabletUiMode.ALWAYS) shouldBe true
        harness.item("Date format").title shouldBe "Date format"
    }

    @Test
    fun previewRowsSubtitle() {
        harness.show(SettingsAppearanceScreen)
        harness.item("Previews row count").subtitle shouldBe "4 rows"
        harness.slide("Previews row count", value = 0)
        koin.ui.previewsRowCount.get() shouldBe 0
        harness.item("Previews row count").subtitle shouldBe "Disabled"
    }
}
