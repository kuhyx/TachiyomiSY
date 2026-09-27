package eu.kanade.presentation.manga.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import eu.kanade.presentation.browse.UiDispatcherReset
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class ScanlatorFilterDialogTest {
    val compose = createComposeRule()

    @get:Rule
    val chain: RuleChain = RuleChain.outerRule(UiDispatcherReset()).around(compose)

    private val events = mutableListOf<String>()

    private fun show(available: Set<String>, excluded: Set<String> = emptySet()) {
        compose.setContent {
            MaterialTheme {
                ScanlatorFilterDialog(
                    availableScanlators = available,
                    excludedScanlators = excluded,
                    onDismissRequest = { events += "dismiss" },
                    onConfirm = { events += "confirm ${it.sorted()}" },
                )
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun noScanlators() {
        show(emptySet())
        compose.onNodeWithText("No scanlators found").assertExists()
        compose.onNodeWithText("Cancel").performClick()
        events shouldContainExactly listOf("dismiss")
    }

    @Test
    fun toggleAndConfirm() {
        show(setOf("beta", "Alpha"), excluded = setOf("beta"))
        compose.onNodeWithText("Alpha").performClick()
        compose.onNodeWithText("beta").performClick()
        compose.onNodeWithText("OK").performClick()
        events shouldContainExactly listOf("confirm [Alpha]", "dismiss")
    }

    @Test
    fun selectAllThenReset() {
        show(setOf("a", "b"))
        compose.onNodeWithText("Select all").performClick()
        compose.onNodeWithText("Reset").performClick()
        compose.onNodeWithText("Select all").assertExists()
        compose.onNodeWithText("Cancel").performClick()
        events shouldContainExactly listOf("dismiss")
    }

    @Test
    fun longListScrolls() {
        show((1..60).map { "Group %02d".format(it) }.toSet())
        compose.onNode(hasScrollAction()).performScrollToIndex(30)
        compose.waitForIdle()
        compose.onNode(hasScrollAction()).performScrollToIndex(59)
        compose.onNodeWithText("Group 60").performClick()
        compose.onNodeWithText("OK").performClick()
        events shouldContainExactly listOf("confirm [Group 60]", "dismiss")
    }
}
