package eu.kanade.presentation.category

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import eu.kanade.tachiyomi.ui.category.CategoryScreenState
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.category.model.Category

@RunWith(RobolectricTestRunner::class)
internal class CategoryScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()

    private fun category(id: Long) = Category(id = id, name = "Cat $id", order = id, flags = 0L)

    private fun show(state: CategoryScreenState.Success) {
        compose.setContent {
            MaterialTheme {
                CategoryScreen(
                    state = state,
                    onClickCreate = { events += "create" },
                    onClickRename = { events += "rename ${it.id}" },
                    onClickDelete = { events += "delete ${it.id}" },
                    onChangeOrder = { cat, to -> events += "order ${cat.id} $to" },
                    navigateUp = { events += "up" },
                )
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun emptyInvitesToCreate() {
        show(CategoryScreenState.Success(emptyList()))
        compose.onNodeWithText("You have no categories", substring = true).assertExists()
        compose.onNodeWithText("Add", useUnmergedTree = true).performClick()
        compose.onNodeWithContentDescription("Navigate up").performClick()
        events shouldContainExactly listOf("create", "up")
    }

    @Test
    fun rowsRenameAndDelete() {
        show(CategoryScreenState.Success(listOf(category(1L), category(2L))))
        compose.onNodeWithText("Cat 1").performClick()
        compose.onAllNodesWithContentDescription("Rename category")[1].performClick()
        compose.onAllNodesWithContentDescription("Delete")[0].performClick()
        events shouldContainExactly listOf("rename 1", "rename 2", "delete 1")
    }

    @Test
    fun newCategoriesReplaceTheList() {
        var state by mutableStateOf(CategoryScreenState.Success(listOf(category(1L))))
        compose.setContent {
            MaterialTheme {
                CategoryScreen(
                    state = state,
                    onClickCreate = {},
                    onClickRename = {},
                    onClickDelete = {},
                    onChangeOrder = { _, _ -> },
                    navigateUp = {},
                )
            }
        }
        compose.onNodeWithText("Cat 1").assertExists()
        state = CategoryScreenState.Success(listOf(category(3L)))
        compose.waitForIdle()
        compose.onNodeWithText("Cat 3").assertExists()
        compose.onNodeWithText("Cat 1").assertDoesNotExist()
    }
}
