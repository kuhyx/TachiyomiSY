package eu.kanade.presentation.category

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import eu.kanade.tachiyomi.ui.category.biometric.BiometricTimesScreenState
import eu.kanade.tachiyomi.ui.category.biometric.TimeRangeItem
import eu.kanade.tachiyomi.ui.category.genre.SortTagScreenState
import eu.kanade.tachiyomi.ui.category.sources.SourceCategoryScreenState
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class CategorySyScreensTest {
    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()
    private val iconButtons = hasClickAction() and
        hasContentDescription("", substring = false) and
        SemanticsMatcher.keyNotDefined(SemanticsProperties.Text)

    private fun showBiometric(state: BiometricTimesScreenState.Success) = compose.setContent {
        MaterialTheme {
            BiometricTimesScreen(
                state = state,
                onClickCreate = { events += "create" },
                onClickDelete = { events += "delete ${it.formattedString}" },
                navigateUp = {},
            )
        }
    }

    private fun showTags(tags: List<String>) = compose.setContent {
        MaterialTheme {
            SortTagScreen(
                state = SortTagScreenState.Success(tags),
                onClickCreate = {},
                onClickDelete = { events += "delete $it" },
                onClickMoveUp = { tag, index -> events += "up $tag $index" },
                onClickMoveDown = { tag, index -> events += "down $tag $index" },
                navigateUp = {},
            )
        }
    }

    private fun showSources(categories: List<String>) = compose.setContent {
        MaterialTheme {
            SourceCategoryScreen(
                state = SourceCategoryScreenState.Success(categories),
                onClickCreate = {},
                onClickRename = { events += "rename $it" },
                onClickDelete = { events += "delete $it" },
                navigateUp = {},
            )
        }
    }

    @Test
    fun biometricEmpty() {
        showBiometric(BiometricTimesScreenState.Success(emptyList()))
        compose.onNodeWithText("You have no biometric lock times", substring = true).assertExists()
    }

    @Test
    fun biometricRowsDelete() {
        showBiometric(BiometricTimesScreenState.Success(listOf(TimeRangeItem(mockk(), "08:00 - 09:00"))))
        compose.onNodeWithText("08:00 - 09:00").assertExists()
        compose.onAllNodes(iconButtons)[0].performClick()
        events shouldContainExactly listOf("delete 08:00 - 09:00")
    }

    @Test
    fun tagsEmpty() {
        showTags(emptyList())
        compose.onNodeWithText("You have no tags", substring = true).assertExists()
    }

    @Test
    fun tagsMoveAndDelete() {
        showTags(listOf("a", "b"))
        // Row a: up (disabled), down, delete; row b: up, down (disabled), delete.
        compose.onAllNodes(iconButtons and isEnabled()).fetchSemanticsNodes().size shouldBe 4
        val buttons = compose.onAllNodes(iconButtons)
        buttons[1].performClick()
        buttons[2].performClick()
        buttons[3].performClick()
        buttons[5].performClick()
        events shouldContainExactly listOf("down a 0", "delete a", "up b 1", "delete b")
    }

    @Test
    fun sourcesEmpty() {
        showSources(emptyList())
        compose.onNodeWithText("No source categories available").assertExists()
    }

    @Test
    fun sourcesRenameAndDelete() {
        showSources(listOf("x"))
        compose.onAllNodesWithText("x")[0].performClick()
        val buttons = compose.onAllNodes(iconButtons)
        buttons[0].performClick()
        buttons[1].performClick()
        events shouldContainExactly listOf("rename x", "rename x", "delete x")
    }
}
