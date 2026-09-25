package exh.ui.batchadd

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import cafe.adriel.voyager.navigator.Navigator
import exh.GalleryAdderHarness
import exh.source.ExhPreferences
import io.mockk.coEvery
import kotlinx.coroutines.CompletableDeferred
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.manga.model.Manga

private const val WAIT_MS = 20_000L

@RunWith(RobolectricTestRunner::class)
internal class BatchAddScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val harness = GalleryAdderHarness()

    @Before
    fun setUp() {
        harness.start()
        loadKoinModules(module { single { ExhPreferences(InMemoryPreferenceStore()) } })
        compose.setContent { MaterialTheme { Navigator(BatchAddScreen()) } }
        compose.waitForIdle()
    }

    @After
    fun tearDown() = harness.stop()

    private fun waitForText(text: String) {
        compose.waitUntil(WAIT_MS) {
            compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun emptyInputShowsTheDialog() {
        compose.onNodeWithText("Batch add").assertExists()
        compose.onNodeWithText("Add Galleries").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("No galleries to add!").assertExists()
        compose.onNodeWithText("You must specify at least one gallery to add!").assertExists()
        compose.onNodeWithText("OK").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("No galleries to add!").assertDoesNotExist()
    }

    @Test
    fun addingShowsProgressThenFinish() {
        val gate = CompletableDeferred<Unit>()
        coEvery { harness.networkToLocalManga(any<Manga>()) } coAnswers {
            gate.await()
            harness.manga
        }
        compose.onNodeWithText("Enter the galleries to add (separated by a new line):").assertExists()
        compose.onNode(hasSetTextAction()).performTextInput("https://gallery.test/g/1/abc")
        compose.onNodeWithText("Add Galleries").performClick()
        waitForText("Adding galleries…")
        compose.onNodeWithText("0/1").assertExists()
        compose.onNodeWithText("Finish").assertDoesNotExist()
        gate.complete(Unit)
        waitForText("Finish")
        compose.onNodeWithText("1/1").assertExists()
        compose.onNodeWithText("Finish").performClick()
        waitForText("Add Galleries")
    }

    @Test
    fun navigatingUpPops() {
        compose.onNodeWithContentDescription("Navigate up").assertExists()
        compose.onNodeWithContentDescription("Navigate up").performClick()
        compose.waitForIdle()
    }
}
