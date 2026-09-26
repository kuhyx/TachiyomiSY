package exh.recs.batch

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

    @Test
    fun forwardedStatusRecomposes() {
        var status by mutableStateOf<SearchStatus>(SearchStatus.Initializing)
        var tick by mutableIntStateOf(0)
        compose.setContent { MaterialTheme { Forwarding(status, tick, { idle++ }, { cancelling++ }) } }
        compose.waitForIdle()
        // The same arguments again, then a new status: the dialog sees each as same, then different.
        tick++
        compose.waitForIdle()
        status = SearchStatus.Error("late")
        compose.waitForIdle()
        compose.onNodeWithText("Search failed").assertIsDisplayed()
        val button = ProgressDialogButton("OK") { idle++ }
        button.copy(text = "Other").text shouldBe "Other"
        (button == button.copy()) shouldBe true
    }

    @Test
    fun unstableStatusRecomposes() {
        // Typed as Processing (it holds an SManga), the call marks the argument unstable for the dialog.
        var status by mutableStateOf(SearchStatus.Processing(SManga(url = "/u", title = "First"), 1, 2))
        var tick by mutableIntStateOf(0)
        compose.setContent {
            tick.hashCode()
            MaterialTheme { RecSearchProgressDialog(status, setStatusIdle = { idle++ }, setStatusCancelling = {}) }
        }
        compose.waitForIdle()
        tick++
        compose.waitForIdle()
        status = SearchStatus.Processing(SManga(url = "/u", title = "Second"), 2, 2)
        compose.waitForIdle()
        compose.onNodeWithText("Second", substring = true).assertIsDisplayed()
    }
}

@Composable
private fun Forwarding(status: SearchStatus, tick: Int, onIdle: () -> Unit, onCancel: () -> Unit) {
    tick.hashCode()
    RecSearchProgressDialog(status = status, setStatusIdle = onIdle, setStatusCancelling = onCancel)
}
