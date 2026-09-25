package eu.kanade.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class AppBarTest {
    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()

    @Test
    fun titleOnlyUsesEveryDefault() {
        compose.setContent { MaterialTheme { AppBar(title = "Plain") } }
        compose.onNodeWithText("Plain").assertExists()
        compose.onNodeWithContentDescription("Navigate up").assertDoesNotExist()
    }

    @Test
    fun fullBarShowsSubtitleAndActions() {
        compose.setContent {
            MaterialTheme {
                AppBar(
                    title = "Title",
                    modifier = Modifier,
                    backgroundColor = Color.Red,
                    subtitle = "Sub",
                    navigateUp = { events += "up" },
                    navigationIcon = Icons.Outlined.Close,
                    actions = { Text("normal") },
                    actionModeCounter = 0,
                    onCancelActionMode = { events += "cancel" },
                    actionModeActions = { Text("mode") },
                    scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
                )
            }
        }
        compose.onNodeWithText("Sub").assertExists()
        compose.onNodeWithText("normal").assertExists()
        compose.onNodeWithText("mode").assertDoesNotExist()
        compose.onNodeWithContentDescription("Navigate up").performClick()
        events shouldContainExactly listOf("up")
    }

    @Test
    fun actionModeShowsTheCounter() {
        compose.setContent {
            MaterialTheme {
                AppBar(
                    title = "Title",
                    actionModeCounter = 3,
                    onCancelActionMode = { events += "cancel" },
                    actionModeActions = { Text("mode") },
                )
            }
        }
        compose.onNodeWithText("3").assertExists()
        compose.onNodeWithText("mode").assertExists()
        compose.onNodeWithContentDescription("Cancel").performClick()
        events shouldContainExactly listOf("cancel")
    }

    @Test
    fun actionModeDefaultsDoNothing() {
        compose.setContent { MaterialTheme { AppBar(title = null, actionModeCounter = 1) } }
        compose.onNodeWithContentDescription("Cancel").performClick()
        compose.onNodeWithText("1").assertExists()
    }

    @Test
    fun contentBarDefaults() {
        compose.setContent { MaterialTheme { AppBar(titleContent = { Text("content") }) } }
        compose.onNodeWithText("content").assertExists()
    }

    @Test
    fun contentBarActionModeDefaults() {
        compose.setContent { MaterialTheme { AppBar(titleContent = {}, isActionMode = true) } }
        compose.onNodeWithContentDescription("Cancel").performClick()
    }

    @Test
    fun titleAndUpIconDefaults() {
        compose.setContent {
            MaterialTheme {
                AppBarTitle(title = "T")
                AppBarTitle(title = null, modifier = Modifier, subtitle = "S")
                UpIcon()
                UpIcon(modifier = Modifier, navigationIcon = Icons.Outlined.Star)
            }
        }
        compose.onNodeWithText("T").assertExists()
        compose.onNodeWithText("S").assertExists()
    }

    @Test
    fun actionModelsAreData() {
        val action = AppBar.Action(title = "a", icon = Icons.Outlined.Star, onClick = {})
        action.copy(enabled = false).enabled shouldBe false
        AppBar.OverflowAction(title = "o", onClick = {}).title shouldBe "o"
    }
}
