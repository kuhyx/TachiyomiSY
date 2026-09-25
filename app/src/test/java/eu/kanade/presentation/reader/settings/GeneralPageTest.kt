package eu.kanade.presentation.reader.settings

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import eu.kanade.presentation.reader.ReaderSettingsHarness
import eu.kanade.presentation.util.setSlider
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import eu.kanade.tachiyomi.util.system.hasDisplayCutout
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class GeneralPageTest {
    @get:Rule
    val compose = createComposeRule()

    private val harness = ReaderSettingsHarness()

    @After
    fun tearDown() = unmockkAll()

    private fun show(noActivity: Boolean = false) {
        compose.setContent {
            MaterialTheme {
                val page = @androidx.compose.runtime.Composable {
                    Column(Modifier.verticalScroll(rememberScrollState())) { GeneralPage(harness.model) }
                }
                if (noActivity) CompositionLocalProvider(LocalActivity provides null) { page() } else page()
            }
        }
        compose.waitForIdle()
    }

    private fun click(text: String) {
        compose.onAllNodesWithText(text)[0].performScrollTo().performClick()
        compose.waitForIdle()
    }

    @Test
    fun themeAndVerticalNavigator() {
        show()
        click("Black")
        click("Paged (left to right)")
        harness.preferences.readerTheme.get() shouldBe 1
        harness.preferences.verticalNavigator.get() shouldBe setOf(ReadingMode.LEFT_TO_RIGHT)
        click("Place vertical navigator on the left side")
        compose.setSlider(index = 0, value = 80f)
        harness.preferences.verticalNavigatorHeight.get() shouldBe 80
        click("Paged (left to right)")
        harness.preferences.verticalNavigator.get() shouldBe emptySet()
    }

    @Test
    fun flashSettingsAppearWhenEnabled() {
        show()
        compose.onNodeWithText("Flash with").assertDoesNotExist()
        click("Flash on page change")
        click("White and Black")
        harness.preferences.flashColor.get() shouldBe ReaderPreferences.FlashColor.WHITE_BLACK
        compose.setSlider(index = 0, value = 5f)
        compose.setSlider(index = 1, value = 3f)
        harness.preferences.flashDurationMillis.get() shouldBe 5 * ReaderPreferences.MILLI_CONVERSION
        harness.preferences.flashPageInterval.get() shouldBe 3
    }

    @Test
    fun cutoutNeedsCutoutAndFullscreen() {
        mockkStatic("eu.kanade.tachiyomi.util.system.DisplayExtensionsKt")
        every { any<android.app.Activity>().hasDisplayCutout() } returns true
        harness.preferences.fullscreen.set(true)
        show()
        compose.onNodeWithText("Show content in cutout area").assertExists()
        click("Fullscreen")
        compose.onNodeWithText("Show content in cutout area").assertDoesNotExist()
    }

    @Test
    fun noCutoutWithoutAnActivity() {
        show(noActivity = true)
        compose.onNodeWithText("Show content in cutout area").assertDoesNotExist()
    }
}
