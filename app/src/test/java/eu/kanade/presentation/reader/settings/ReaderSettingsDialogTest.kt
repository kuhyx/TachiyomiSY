package eu.kanade.presentation.reader.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import eu.kanade.presentation.reader.ReaderSettingsHarness
import eu.kanade.presentation.reader.readerManga
import eu.kanade.presentation.util.ProvideBack
import eu.kanade.presentation.util.TestBackOwner
import eu.kanade.tachiyomi.ui.reader.setting.ReaderOrientation
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ReaderSettingsDialogTest {
    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()

    private val back = TestBackOwner()

    private fun show(harness: ReaderSettingsHarness) {
        compose.setContent {
            ProvideBack(back) {
            MaterialTheme {
                ReaderSettingsDialog(
                    onDismissRequest = { events += "dismiss" },
                    onShowMenus = { events += "show" },
                    onHideMenus = { events += "hide" },
                    screenModel = harness.model,
                )
            }
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun readingModeTabChangesTheSeries() {
        val harness = ReaderSettingsHarness(manga = readerManga())
        show(harness)
        compose.onNodeWithText("Paged (right to left)").performClick()
        compose.onNodeWithText("Portrait").performClick()
        harness.modes shouldContainExactly listOf(ReadingMode.RIGHT_TO_LEFT)
        harness.orientations shouldContainExactly listOf(ReaderOrientation.PORTRAIT)
    }

    @Test
    fun colorFilterTabHidesTheMenus() {
        show(ReaderSettingsHarness())
        compose.onNodeWithText("Custom filter").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Grayscale").assertExists()
        compose.onAllNodesWithText("General")[0].performClick()
        compose.waitForIdle()
        events shouldContainExactly listOf("show", "hide", "show")
    }

    @Test
    fun backDismissesAndShowsMenus() {
        show(ReaderSettingsHarness())
        compose.runOnIdle { back.pressBack() }
        compose.waitForIdle()
        events shouldContainExactly listOf("show", "dismiss", "show")
    }
}
