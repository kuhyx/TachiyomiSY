package tachiyomi.presentation.core.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.i18n.MR

@RunWith(RobolectricTestRunner::class)
internal class EmptyScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private var clicks = 0

    private val retry = EmptyScreenAction(
        stringRes = MR.strings.action_retry,
        icon = Icons.Default.Refresh,
        onClick = { clicks++ },
    )

    @Test
    fun resourceMessageWithDefaults() {
        compose.setContent { MaterialTheme { EmptyScreen(MR.strings.no_results_found) } }
        compose.onNodeWithText("No results found").assertIsDisplayed()
        compose.onNodeWithText("Retry").assertDoesNotExist()
    }

    @Test
    fun resourceMessageWithActions() {
        compose.setContent {
            MaterialTheme {
                EmptyScreen(
                    stringRes = MR.strings.no_results_found,
                    modifier = Modifier.testTag("empty"),
                    actions = listOf(retry),
                )
            }
        }
        compose.onNodeWithTag("empty").assertExists()
        compose.onNodeWithText("Retry").performClick()
        compose.runOnIdle { clicks shouldBe 1 }
    }

    @Test
    fun plainMessageWithDefaults() {
        compose.setContent { MaterialTheme { EmptyScreen(message = "Nothing here") } }
        compose.onNodeWithText("Nothing here").assertIsDisplayed()
    }

    @Test
    fun plainMessageWithEmptyActions() {
        compose.setContent {
            MaterialTheme {
                EmptyScreen(message = "Nothing here", modifier = Modifier.testTag("empty"), actions = emptyList())
            }
        }
        compose.onNodeWithTag("empty").assertExists()
        compose.onNodeWithText("Retry").assertDoesNotExist()
    }

    @Test
    fun plainMessageWithTwoActions() {
        val cancel = EmptyScreenAction(stringRes = MR.strings.action_cancel, icon = Icons.Default.Refresh, onClick = {})
        compose.setContent { MaterialTheme { EmptyScreen(message = "Nothing here", actions = listOf(retry, cancel)) } }
        compose.onNodeWithText("Cancel").assertIsDisplayed()
        compose.onNodeWithText("Retry").performClick()
        compose.runOnIdle { clicks shouldBe 1 }
    }
}
