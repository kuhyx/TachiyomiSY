package eu.kanade.presentation.category.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.i18n.MR

@RunWith(RobolectricTestRunner::class)
internal class CategoryDialogsTest {
    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()

    @Test
    fun createNeedsAFreshName() {
        compose.setContent {
            MaterialTheme {
                CategoryCreateDialog(
                    onDismissRequest = { events += "dismiss" },
                    onCreate = { events += "create $it" },
                    categories = listOf("Taken"),
                )
            }
        }
        compose.mainClock.advanceTimeBy(500L)
        compose.onNodeWithText("Add category").assertExists()
        compose.onNodeWithText("*required").assertExists()
        compose.onNodeWithText("Add").assertIsNotEnabled()
        compose.onNode(hasSetTextAction()).performTextInput("Taken")
        compose.onNodeWithText("A category with this name already exists!").assertExists()
        compose.onNode(hasSetTextAction()).performTextClearance()
        compose.onNode(hasSetTextAction()).performTextInput("New")
        compose.onNodeWithText("Add").assertIsEnabled().performClick()
        events shouldContainExactly listOf("create New", "dismiss")
    }

    @Test
    fun createWithCustomTexts() {
        compose.setContent {
            MaterialTheme {
                CategoryCreateDialog(
                    onDismissRequest = { events += "dismiss" },
                    onCreate = {},
                    categories = listOf("Taken"),
                    title = "New tag",
                    extraMessage = "Extra",
                    alreadyExistsError = MR.strings.error_category_exists,
                )
            }
        }
        compose.onNodeWithText("New tag").assertExists()
        compose.onNodeWithText("Extra").assertExists()
        compose.onNodeWithText("Cancel").performClick()
        events shouldContainExactly listOf("dismiss")
    }

    @Test
    fun renameNeedsAChange() {
        compose.setContent {
            MaterialTheme {
                CategoryRenameDialog(
                    onDismissRequest = { events += "dismiss" },
                    onRename = { events += "rename $it" },
                    categories = listOf("Old", "Other"),
                    category = "Old",
                )
            }
        }
        compose.mainClock.advanceTimeBy(500L)
        compose.onNodeWithText("OK").assertIsNotEnabled()
        compose.onNode(hasSetTextAction()).performTextClearance()
        compose.onNode(hasSetTextAction()).performTextInput("Other")
        compose.onNodeWithText("A category with this name already exists!").assertExists()
        compose.onNode(hasSetTextAction()).performTextClearance()
        compose.onNode(hasSetTextAction()).performTextInput("Fresh")
        compose.onNodeWithText("*required").assertExists()
        compose.onNodeWithText("OK").performClick()
        compose.onNodeWithText("Cancel").performClick()
        events shouldContainExactly listOf("rename Fresh", "dismiss", "dismiss")
    }

    @Test
    fun deleteDefaultsNameTheCategory() {
        compose.setContent {
            MaterialTheme {
                CategoryDeleteDialog(onDismissRequest = { events += "dismiss" }, onDelete = { events += "delete" })
            }
        }
        compose.onNodeWithText("Delete category").assertExists()
        compose.onNodeWithText("Do you wish to delete the category \"\"?").assertExists()
        compose.onNodeWithText("OK").performClick()
        compose.onNodeWithText("Cancel").performClick()
        events shouldContainExactly listOf("delete", "dismiss", "dismiss")
    }

    @Test
    fun deleteWithCustomTexts() {
        compose.setContent {
            MaterialTheme {
                CategoryDeleteDialog(
                    onDismissRequest = {},
                    onDelete = {},
                    category = "Cat",
                    title = "Drop it",
                    text = "Really?",
                )
            }
        }
        compose.onNodeWithText("Drop it").assertExists()
        compose.onNodeWithText("Really?").assertExists()
    }
}
