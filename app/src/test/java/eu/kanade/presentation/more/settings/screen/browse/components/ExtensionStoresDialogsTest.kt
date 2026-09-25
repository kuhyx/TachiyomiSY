package eu.kanade.presentation.more.settings.screen.browse.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val URL = "https://store.example/index.min.json"

@RunWith(RobolectricTestRunner::class)
internal class ExtensionStoresDialogsTest {
    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()

    private fun create(processing: Boolean, error: String?) {
        compose.setContent {
            MaterialTheme {
                ExtensionStoreCreateDialog(
                    onDismissRequest = { events += "dismiss" },
                    onCreate = { events += "create:$it" },
                    storeIndexUrls = setOf(URL),
                    processing = processing,
                    errorMessage = error,
                )
            }
        }
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
    }

    private fun confirm(exists: Boolean, processing: Boolean, error: String?) {
        compose.setContent {
            MaterialTheme {
                ExtensionStoreConfirmDialog(
                    onDismissRequest = { events += "dismiss" },
                    onCreate = { events += "create" },
                    storeIndexUrl = URL,
                    storeAlreadyExists = exists,
                    processing = processing,
                    errorMessage = error,
                )
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun createValidatesUrl() {
        create(processing = false, error = null)
        compose.onNodeWithText("*required").assertExists()
        compose.onNodeWithText("Add").assertIsNotEnabled()
        compose.onNode(hasSetTextAction()).performTextReplacement(URL)
        compose.onNodeWithText("This extension store already exists").assertExists()
        compose.onNode(hasSetTextAction()).performTextReplacement("https://other.example")
        compose.onNodeWithText("Add").performClick()
        compose.onNodeWithText("Cancel").performClick()
        events shouldBe listOf("create:https://other.example", "dismiss")
    }

    @Test
    fun createProcessingWithError() {
        create(processing = true, error = "boom")
        compose.onNodeWithText("boom").assertExists()
        compose.onNodeWithText("Processing…").assertExists()
    }

    @Test
    fun deleteConfirms() {
        compose.setContent {
            MaterialTheme {
                ExtensionStoreDeleteDialog(
                    onDismissRequest = { events += "dismiss" },
                    onDelete = { events += "delete" },
                    storeName = "Store",
                    storeIndexUrl = URL,
                )
            }
        }
        compose.onNodeWithText("OK").performClick()
        compose.onNodeWithText("Cancel").performClick()
        events shouldBe listOf("delete", "dismiss", "dismiss")
    }

    @Test
    fun confirmExisting() {
        confirm(exists = true, processing = false, error = null)
        compose.onNodeWithText("This extension store already exists").assertExists()
        compose.onNodeWithText("Add").assertIsNotEnabled()
    }

    @Test
    fun confirmWithError() {
        confirm(exists = false, processing = true, error = "bad index")
        compose.onNodeWithText("bad index").assertExists()
        compose.onNodeWithText("Processing…").assertExists()
    }

    @Test
    fun confirmAdds() {
        confirm(exists = false, processing = false, error = null)
        compose.onNodeWithText("Add").performClick()
        compose.onNodeWithText("Cancel").performClick()
        events shouldBe listOf("create", "dismiss")
    }
}
