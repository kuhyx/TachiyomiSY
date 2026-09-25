package eu.kanade.presentation.more.settings.widget

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class TriStateListDialogTest {
    @get:Rule
    val compose = createComposeRule()

    private var result: Pair<List<String>, List<String>>? = null
    private var isDismissed = false

    private fun show(items: List<String>, message: String?) {
        compose.setContent {
            MaterialTheme {
                TriStateListDialog(
                    title = "Tri",
                    message = message,
                    items = items,
                    initialChecked = listOf("a"),
                    initialInversed = listOf("b"),
                    onDismissRequest = { isDismissed = true },
                    onValueChanged = { included, excluded -> result = included to excluded },
                    itemLabel = { it.uppercase() },
                )
            }
        }
        compose.waitForIdle()
    }

    private fun described(text: String): Int =
        compose.onAllNodesWithContentDescription(text).fetchSemanticsNodes().size

    @Test
    fun rowsCycleThroughStates() {
        show(items = listOf("a", "b", "c"), message = "Hint")
        compose.onNodeWithText("Hint").assertExists()
        described("Selected") shouldBe 1
        described("Disabled") shouldBe 1
        described("Not selected") shouldBe 1
        compose.onNodeWithText("A").performClick()
        compose.onNodeWithText("B").performClick()
        compose.onNodeWithText("C").performClick()
        compose.onNodeWithText("OK").performClick()
        result shouldBe (listOf("c") to listOf("a"))
    }

    @Test
    fun cancelDismisses() {
        show(items = listOf("a"), message = null)
        compose.onNodeWithText("Cancel").performClick()
        isDismissed shouldBe true
    }

    @Test
    fun longListShowsDividers() {
        show(items = (1..40).map { "item$it" }, message = null)
        val list = compose.onAllNodes(hasScrollToIndexAction()).onFirst()
        list.performScrollToIndex(39)
        compose.waitForIdle()
        list.performScrollToIndex(20)
        compose.waitForIdle()
        compose.onNodeWithText("ITEM21").assertExists()
    }
}
