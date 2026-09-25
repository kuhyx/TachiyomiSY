package eu.kanade.presentation.reader

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ReaderPageActionsDialogTest {
    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()

    private fun show(extraPage: Boolean) {
        compose.setContent {
            MaterialTheme {
                ReaderPageActionsDialog(
                    onDismissRequest = { events += "dismiss" },
                    onSetAsCover = { events += "cover $it" },
                    onShare = { copy, extra -> events += "share $copy $extra" },
                    onSave = { events += "save $it" },
                    onShareCombined = { events += "combined $it" },
                    onSaveCombined = { events += "save combined" },
                    hasExtraPage = extraPage,
                )
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun singlePageActions() {
        show(extraPage = false)
        compose.onNodeWithText("Copy to clipboard").performClick()
        compose.onNodeWithText("Share").performClick()
        compose.onNodeWithText("Save").performClick()
        compose.onNodeWithText("Set as cover").performClick()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Set as cover").performClick()
        compose.onNodeWithText("OK").performClick()
        events shouldContainExactly listOf(
            "share true false",
            "dismiss",
            "share false false",
            "dismiss",
            "save false",
            "dismiss",
            "cover false",
        )
    }

    @Test
    fun spreadActions() {
        show(extraPage = true)
        compose.onNodeWithText("Copy second page").performClick()
        compose.onNodeWithText("Share second page").performClick()
        compose.onNodeWithText("Save first page").performClick()
        compose.onNodeWithText("Copy combined page").performClick()
        compose.onNodeWithText("Share combined page").performClick()
        compose.onNodeWithText("Save combined page").performClick()
        compose.onNodeWithText("Set first page as cover").performClick()
        compose.onNodeWithText("OK").performClick()
        events shouldContainExactly listOf(
            "share true true",
            "dismiss",
            "share false true",
            "dismiss",
            "save false",
            "dismiss",
            "combined true",
            "dismiss",
            "combined false",
            "dismiss",
            "save combined",
            "dismiss",
            "cover false",
        )
    }
}
