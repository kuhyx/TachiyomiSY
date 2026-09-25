package eu.kanade.presentation.history.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class HistoryDialogsTest {
    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()

    private fun showDelete() {
        compose.setContent {
            MaterialTheme {
                HistoryDeleteDialog(onDismissRequest = { events += "dismiss" }, onDelete = { events += "delete $it" })
            }
        }
    }

    @Test
    fun deleteKeepsOtherChapters() {
        showDelete()
        compose.onAllNodesWithText("Remove").fetchSemanticsNodes().size shouldBe 2
        compose.onNodeWithText("Cancel").performClick()
        events shouldContainExactly listOf("dismiss")
    }

    @Test
    fun deleteCanResetEverything() {
        showDelete()
        compose.onNodeWithText("Reset all chapters for this entry").performClick()
        compose.onAllNodesWithText("Remove")[1].performClick()
        events shouldContainExactly listOf("delete true", "dismiss")
    }

    @Test
    fun deleteAllConfirms() {
        compose.setContent {
            MaterialTheme {
                HistoryDeleteAllDialog(onDismissRequest = { events += "dismiss" }, onDelete = { events += "all" })
            }
        }
        compose.onNodeWithText("Are you sure? All history will be lost.").assertExists()
        compose.onNodeWithText("OK").performClick()
        compose.onNodeWithText("Cancel").performClick()
        events shouldContainExactly listOf("all", "dismiss", "dismiss")
    }

    @Test
    fun previewRenders() {
        compose.setContent { HistoryDeleteDialogPreview() }
        compose.onNodeWithText("Reset all chapters for this entry").assertExists()
    }
}
