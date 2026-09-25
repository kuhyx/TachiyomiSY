package eu.kanade.presentation.reader.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import eu.kanade.presentation.reader.ReaderSettingsHarness
import eu.kanade.presentation.util.setSlider
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ColorFilterPageTest {
    @get:Rule
    val compose = createComposeRule()

    private val harness = ReaderSettingsHarness()

    private fun click(text: String) {
        compose.onAllNodesWithText(text)[0].performScrollTo().performClick()
        compose.waitForIdle()
    }

    @Test
    fun brightnessAndChannelsWriteThePreferences() {
        compose.setContent {
            MaterialTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) { ColorFilterPage(harness.model) }
            }
        }
        click("Custom brightness")
        compose.setSlider(index = 0, value = -20f)
        harness.preferences.customBrightnessValue.get() shouldBe -20
        click("Custom color filter")
        harness.preferences.colorFilterValue.set(0)
        compose.waitForIdle()
        compose.setSlider(index = 1, value = 1f)
        compose.setSlider(index = 2, value = 1f)
        compose.setSlider(index = 3, value = 1f)
        compose.setSlider(index = 4, value = 1f)
        harness.preferences.colorFilterValue.get() shouldBe 0x01010101
        click("Multiply")
        harness.preferences.colorFilterMode.get() shouldBe 1
        click("Grayscale")
        harness.preferences.grayscale.get() shouldBe true
    }
}
