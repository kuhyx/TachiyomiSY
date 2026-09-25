package exh.recs.batch

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import eu.kanade.tachiyomi.source.model.SManga
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class RecSearchProgressDialogTest {
    @get:Rule
    val compose = createComposeRule()

    private var idle = 0
    private var cancelling = 0

    private fun show(status: SearchStatus) {
        compose.setContent {
            MaterialTheme {
                RecSearchProgressDialog(
                    status = status,
                    setStatusIdle = { idle++ },
                    setStatusCancelling = { cancelling++ },
                )
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun initializingShowsCancel() {
        show(SearchStatus.Initializing)
        compose.onNodeWithText("Collecting recommendations").assertIsDisplayed()
        compose.onNodeWithText("Initializing").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        compose.waitForIdle()
        cancelling shouldBe 1
        idle shouldBe 0
    }

    @Test
    fun errorShowsOk() {
        show(SearchStatus.Error("no network"))
        compose.onNodeWithText("Search failed").assertIsDisplayed()
        compose.onNodeWithText("An error occurred during the search process: no network").assertIsDisplayed()
        compose.onNodeWithText("OK").performClick()
        compose.waitForIdle()
        idle shouldBe 1
    }

    @Test
    fun processingShowsProgress() {
        show(SearchStatus.Processing(SManga(url = "/u", title = "Entry Title"), current = 2, total = 5))
        compose.onNodeWithText("Processing entry 2 of 5", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Entry Title", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        compose.waitForIdle()
        cancelling shouldBe 1
    }

    @Test
    fun statusesWithoutADialog() {
        // One composition, several statuses: `setContent` may only be called once per test.
        val status = mutableStateOf<SearchStatus>(SearchStatus.Idle)
        compose.setContent {
            MaterialTheme {
                RecSearchProgressDialog(
                    status = status.value,
                    setStatusIdle = { idle++ },
                    setStatusCancelling = { cancelling++ },
                )
            }
        }
        for (quiet in listOf(SearchStatus.Idle, SearchStatus.Finished.WithoutResults, SearchStatus.Cancelling)) {
            status.value = quiet
            compose.waitForIdle()
            compose.onAllNodesWithText("Collecting recommendations").fetchSemanticsNodes().isEmpty() shouldBe true
        }
        status.value = SearchStatus.Initializing
        compose.waitForIdle()
        compose.onNodeWithText("Collecting recommendations").assertIsDisplayed()
    }
}
