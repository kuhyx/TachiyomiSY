package eu.kanade.presentation.category.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.preference.CheckboxState
import tachiyomi.domain.category.model.Category

@RunWith(RobolectricTestRunner::class)
internal class ChangeCategoryDialogTest {
    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()

    private fun category(id: Long) = Category(id = id, name = "Cat $id", order = id, flags = 0L)

    private fun show(selection: List<CheckboxState<Category>>) {
        compose.setContent {
            MaterialTheme {
                ChangeCategoryDialog(
                    initialSelection = selection,
                    onDismissRequest = { events += "dismiss" },
                    onEditCategories = { events += "edit" },
                    onConfirm = { include, exclude -> events += "confirm $include $exclude" },
                )
            }
        }
    }

    @Test
    fun noCategoriesOffersToEdit() {
        show(emptyList())
        compose.onNodeWithText("You don't have any categories yet.").assertExists()
        compose.onNodeWithText("Edit categories").performClick()
        events shouldContainExactly listOf("dismiss", "edit")
    }

    @Test
    fun checkboxesCycleStates() {
        show(
            listOf(
                CheckboxState.State.None(category(1L)),
                CheckboxState.State.Checked(category(2L)),
                CheckboxState.TriState.None(category(3L)),
                CheckboxState.TriState.Include(category(4L)),
            ),
        )
        compose.onNodeWithText("Cat 1").performClick()
        compose.onAllNodes(isToggleable())[1].performClick()
        compose.onNodeWithText("Cat 3").performClick()
        compose.onAllNodes(isToggleable())[3].performClick()
        compose.onNodeWithText("OK").performClick()
        events shouldContainExactly listOf("dismiss", "confirm [1, 3] [2]")
    }

    @Test
    fun editAndCancel() {
        show(listOf(CheckboxState.State.Checked(category(0L))))
        compose.onNodeWithText("Default").assertExists()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Edit").performClick()
        events shouldContainExactly listOf("dismiss", "dismiss", "edit")
    }

    @Test
    fun idsSplitByState() {
        val states = listOf(
            CheckboxState.State.Checked(category(1L)),
            CheckboxState.State.None(category(2L)),
            CheckboxState.TriState.Include(category(3L)),
            CheckboxState.TriState.Exclude(category(4L)),
            CheckboxState.TriState.None(category(5L)),
        )
        states.includedIds() shouldContainExactly listOf(1L, 3L)
        states.excludedIds() shouldContainExactly listOf(2L, 5L)
    }
}
