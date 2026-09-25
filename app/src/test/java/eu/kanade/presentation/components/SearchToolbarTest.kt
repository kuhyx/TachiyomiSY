package eu.kanade.presentation.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class SearchToolbarTest {
    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()
    private var query by mutableStateOf<String?>(null)

    private fun show(noKeyboard: Boolean = false) {
        compose.setContent {
            MaterialTheme {
                val content = @androidx.compose.runtime.Composable {
                    SearchToolbar(
                        searchQuery = query,
                        onChangeSearchQuery = {
                            query = it
                            events += "query $it"
                        },
                        titleContent = { Text("Title") },
                        navigateUp = { events += "up" },
                        onSearch = { events += "search $it" },
                    )
                }
                if (noKeyboard) {
                    CompositionLocalProvider(LocalSoftwareKeyboardController provides null) { content() }
                } else {
                    content()
                }
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun closedShowsTitleAndSearch() {
        show()
        compose.onNodeWithText("Title").assertExists()
        compose.onNodeWithContentDescription("Navigate up").performClick()
        compose.onNodeWithContentDescription("Search").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Search…").assertExists()
        events shouldContainExactly listOf("up", "query ")
    }

    @Test
    fun typingReportsAndResets() {
        query = ""
        show()
        compose.onNode(hasSetTextAction()).performTextInput("abc")
        compose.waitForIdle()
        query shouldBe "abc"
        compose.onNodeWithContentDescription("Reset").performClick()
        compose.waitForIdle()
        query shouldBe ""
        compose.onNodeWithText("Search…").assertExists()
    }

    @Test
    fun imeSearchSubmitsNonBlank() {
        query = "  "
        show()
        compose.onNode(hasSetTextAction()).performImeAction()
        query = "needle"
        compose.waitForIdle()
        compose.onNode(hasSetTextAction()).performImeAction()
        compose.waitForIdle()
        events shouldContainExactly listOf("search needle")
    }

    @Test
    fun enterKeySubmitsWithoutKeyboard() {
        query = "needle"
        show(noKeyboard = true)
        compose.onNode(hasSetTextAction()).performKeyInput { pressKey(Key.Enter) }
        compose.waitForIdle()
        events shouldContainExactly listOf("search needle")
    }

    @Test
    fun closeSearchClearsTheQuery() {
        query = "x"
        show()
        compose.onNodeWithContentDescription("Navigate up").performClick()
        compose.waitForIdle()
        query shouldBe null
        compose.onNodeWithText("Title").assertExists()
    }

    @Test
    fun disabledSearchExplicitArgs() {
        compose.setContent {
            MaterialTheme {
                SearchToolbar(
                    searchQuery = "",
                    onChangeSearchQuery = {},
                    modifier = Modifier,
                    searchEnabled = false,
                    placeholderText = "Find it",
                    onClickCloseSearch = { events += "close" },
                    actions = { Text("extra") },
                    scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
                    interactionSource = MutableInteractionSource(),
                )
            }
        }
        compose.onNodeWithText("Find it").assertExists()
        compose.onNodeWithText("extra").assertExists()
        compose.onNodeWithContentDescription("Search").assertDoesNotExist()
        compose.onNodeWithContentDescription("Navigate up").performClick()
        events shouldContainExactly listOf("close")
    }

    @Test
    fun defaultsOnlyNeedTheQuery() {
        compose.setContent { MaterialTheme { SearchToolbar(searchQuery = null, onChangeSearchQuery = {}) } }
        compose.onNodeWithContentDescription("Navigate up").assertDoesNotExist()
    }
}
