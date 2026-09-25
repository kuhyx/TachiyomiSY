package eu.kanade.presentation.reader.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import eu.kanade.presentation.reader.ReaderSettingsHarness
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonViewer
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ReaderPagesTest {
    @get:Rule
    val compose = createComposeRule()

    private fun readingModePage(harness: ReaderSettingsHarness) {
        compose.setContent {
            MaterialTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) { ReadingModePage(harness.model) }
            }
        }
        compose.waitForIdle()
    }

    private fun click(text: String, index: Int = 0) {
        compose.onAllNodesWithText(text)[index].performScrollTo().performClick()
        compose.waitForIdle()
    }

    @Test
    fun pagerSettingsWriteThePreferences() {
        val harness = ReaderSettingsHarness()
        readingModePage(harness)
        compose.onNodeWithText("Paged").assertExists()
        click("Split wide pages")
        click("Invert split page placement")
        click("Rotate wide pages to fit")
        click("Flip orientation of rotated wide pages")
        harness.preferences.dualPageSplitPaged.get() shouldBe true
        harness.preferences.dualPageRotateToFitInvert.get() shouldBe true
    }

    @Test
    fun tapZonesAndInvertModes() {
        val harness = ReaderSettingsHarness()
        readingModePage(harness)
        click("L shaped")
        click("Horizontal")
        click("Stretch")
        click("Left")
        click("Double pages")
        click("Add to wide Page")
        harness.preferences.navigationModePager.get() shouldBe 1
        harness.preferences.pagerNavInverted.get() shouldBe ReaderPreferences.TappingInvertMode.HORIZONTAL
        harness.preferences.imageScaleType.get() shouldBe 2
        harness.preferences.pageLayout.get() shouldBe 1
        harness.preferences.navigationModePager.set(5)
        compose.waitForIdle()
        compose.onNodeWithText("Invert tap zones").assertDoesNotExist()
    }

    @Test
    fun webtoonViewerShowsItsSettings() {
        val harness = ReaderSettingsHarness(viewer = mockk<WebtoonViewer>())
        readingModePage(harness)
        compose.onNodeWithText("Long strip with gaps").assertExists()
        compose.onNodeWithText("Side padding").assertExists()
        click("Split wide pages")
        click("Rotate wide pages to fit")
        click("L shaped")
        click("Horizontal")
        harness.preferences.dualPageSplitWebtoon.get() shouldBe true
        harness.preferences.navigationModeWebtoon.get() shouldBe 1
        harness.preferences.webtoonNavInverted.get() shouldBe ReaderPreferences.TappingInvertMode.HORIZONTAL
    }
}
