package eu.kanade.presentation.more.settings.widget

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.espresso.Espresso
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ListPreferenceWidgetTest {
    @get:Rule
    val compose = createComposeRule()

    private var picked: String? = null

    private fun show(entries: Map<String, String>) {
        compose.setContent {
            var value by remember { mutableStateOf("a") }
            MaterialTheme {
                ListPreferenceWidget(
                    value = value,
                    title = "Pick",
                    subtitle = "Current $value",
                    icon = null,
                    entries = entries,
                    onValueChange = {
                        picked = it
                        value = it
                    },
                )
            }
        }
        compose.onNodeWithText("Pick").performClick()
        compose.waitForIdle()
    }

    private fun count(text: String): Int = compose.onAllNodesWithText(text).fetchSemanticsNodes().size

    @Test
    fun selectingOtherEntryReports() {
        show(mapOf("a" to "Alpha", "b" to "Beta"))
        compose.onNodeWithText("Beta").performClick()
        compose.waitForIdle()
        picked shouldBe "b"
        count("Alpha") shouldBe 0
        compose.onNodeWithText("Current b").assertExists()
    }

    @Test
    fun selectedEntryIsNoOp() {
        show(mapOf("a" to "Alpha", "b" to "Beta"))
        compose.onNodeWithText("Alpha").performClick()
        compose.waitForIdle()
        picked shouldBe null
        count("Alpha") shouldBe 1
    }

    @Test
    fun cancelClosesDialog() {
        show(mapOf("a" to "Alpha"))
        compose.onNodeWithText("Cancel").performClick()
        compose.waitForIdle()
        count("Alpha") shouldBe 0
    }

    @Test
    fun backDismissesDialog() {
        show(mapOf("a" to "Alpha"))
        Espresso.pressBackUnconditionally()
        compose.waitForIdle()
        count("Alpha") shouldBe 0
    }

    @Test
    fun longListShowsDividers() {
        show((1..40).associate { "k$it" to "Entry $it" })
        val list = compose.onAllNodes(hasScrollToIndexAction()).onFirst()
        list.performScrollToIndex(39)
        compose.waitForIdle()
        list.performScrollToIndex(20)
        compose.waitForIdle()
        count("Entry 20") shouldBe 1
    }
}
