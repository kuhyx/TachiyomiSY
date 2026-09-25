package eu.kanade.presentation.track

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class TrackerSearchTest {
    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()

    private fun show(
        state: TextFieldState = TextFieldState(),
        result: Result<List<TrackSearch>>? = null,
        selected: TrackSearch? = null,
        private: Boolean = false,
    ) {
        compose.setContent {
            MaterialTheme {
                TrackerSearch(
                    state = state,
                    onDispatchQuery = { events += "query" },
                    queryResult = result,
                    selected = selected,
                    onSelectedChange = { events += "select ${it.title}" },
                    onConfirmSelection = { events += "confirm $it" },
                    onDismissRequest = { events += "dismiss" },
                    supportsPrivateTracking = private,
                )
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun emptyQueryShowsTheHint() {
        show()
        compose.onNodeWithText("Search…").assertExists()
        compose.onNode(hasSetTextAction()).performImeAction()
        compose.onNode(hasSetTextAction()).performKeyInput { pressKey(Key.Enter) }
        events shouldContainExactly listOf("query", "query")
    }

    @Test
    fun clearButtonEmptiesTheField() {
        val state = TextFieldState(initialText = "naruto")
        show(state = state)
        compose.onNodeWithText("Search…").assertDoesNotExist()
        compose.onNodeWithText("naruto").assertExists()
        compose.onAllNodes(androidx.compose.ui.test.hasClickAction())[1].performClick()
        compose.waitForIdle()
        state.text.toString() shouldBe ""
        compose.onAllNodes(androidx.compose.ui.test.hasClickAction())[0].performClick()
        events shouldContainExactly listOf("dismiss")
    }

    @Test
    fun selectedResultCanBeTracked() {
        val result = trackSearch("Picked")
        show(result = Result.success(listOf(result)), selected = result)
        compose.onNodeWithText("Track").performClick()
        compose.onNodeWithContentDescription("Track privately").assertDoesNotExist()
        events shouldContainExactly listOf("confirm false")
    }

    @Test
    fun privateTrackingButton() {
        val result = trackSearch("Picked")
        show(result = Result.success(listOf(result)), selected = result, private = true)
        compose.onNodeWithContentDescription("Track privately").performClick()
        events shouldContainExactly listOf("confirm true")
    }
}
