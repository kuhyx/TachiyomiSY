package eu.kanade.presentation.reader

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import eu.kanade.presentation.reader.components.Preview
import eu.kanade.tachiyomi.ui.reader.setting.ReaderOrientation
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ModeSelectionDialogsTest {
    @get:Rule
    val compose = createComposeRule()

    private val changes = mutableListOf<String>()
    private var dismissed = 0

    @Test
    fun readingModeAppliesTheSelection() {
        val harness = ReaderSettingsHarness(manga = readerManga())
        compose.setContent {
            MaterialTheme {
                ReadingModeSelectDialog(
                    onDismissRequest = { dismissed++ },
                    screenModel = harness.model,
                    onChange = { changes += it.toString() },
                )
            }
        }
        compose.onNodeWithText("Paged (right to left)").performClick()
        compose.onNodeWithText("Apply").performClick()
        harness.modes shouldContainExactly listOf(ReadingMode.RIGHT_TO_LEFT)
        dismissed shouldBe 1
        changes.size shouldBe 1
    }

    @Test
    fun readingModeRevertsToDefault() {
        val harness = ReaderSettingsHarness(manga = readerManga())
        compose.setContent {
            MaterialTheme { ReadingModeSelectDialog(onDismissRequest = {}, screenModel = harness.model, onChange = {}) }
        }
        compose.onNodeWithText("Revert to default").performClick()
        harness.modes shouldContainExactly listOf(ReadingMode.DEFAULT)
    }

    @Test
    fun orientationAppliesAndReverts() {
        val harness = ReaderSettingsHarness(manga = readerManga())
        compose.setContent {
            MaterialTheme {
                OrientationSelectDialog(onDismissRequest = { dismissed++ }, screenModel = harness.model, onChange = {})
            }
        }
        compose.onNodeWithText("Portrait").performClick()
        compose.onNodeWithText("Apply").performClick()
        compose.onNodeWithText("Revert to default").performClick()
        harness.orientations shouldContainExactly listOf(ReaderOrientation.PORTRAIT, ReaderOrientation.DEFAULT)
    }

    @Test
    fun previewsRender() {
        compose.setContent {
            Column {
                ReadingModeContentPreview()
                OrientationContentPreview()
                Preview()
            }
        }
        compose.onAllNodesWithText("Revert to default").fetchSemanticsNodes().size shouldBe 3
    }
}
