package tachiyomi.presentation.core.components.material

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class AlertDialogContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun buttonsAndTextOnly() {
        compose.setContent {
            MaterialTheme {
                AlertDialogContent(buttons = { Text("ok") }, text = { Text("body") })
            }
        }
        compose.onNodeWithText("ok").assertIsDisplayed()
        compose.onNodeWithText("body").assertIsDisplayed()
    }

    @Test
    fun buttonsWithIconAndTitle() {
        compose.setContent {
            MaterialTheme {
                AlertDialogContent(
                    buttons = { Text("ok") },
                    modifier = Modifier.testTag("dialog"),
                    icon = { Text("icon") },
                    title = { Text("title") },
                    text = { Text("body") },
                )
            }
        }
        compose.onNodeWithTag("dialog").assertWidthIsAtLeast(280.dp)
        compose.onNodeWithText("icon").assertIsDisplayed()
        compose.onNodeWithText("title").assertIsDisplayed()
        compose.onNodeWithText("body").assertIsDisplayed()
        compose.onNodeWithText("ok").assertIsDisplayed()
    }

    @Test
    fun emptyContent() {
        compose.setContent {
            MaterialTheme {
                AlertDialogContent(modifier = Modifier.testTag("empty"))
            }
        }
        compose.onNodeWithTag("empty").assertWidthIsAtLeast(280.dp)
    }

    @Test
    fun iconOnly() {
        compose.setContent {
            MaterialTheme {
                AlertDialogContent(icon = { Text("icon") }, content = { Text("free") })
            }
        }
        compose.onNodeWithText("icon").assertIsDisplayed()
        compose.onNodeWithText("free").assertIsDisplayed()
    }

    @Test
    fun titleOnly() {
        compose.setContent {
            MaterialTheme {
                AlertDialogContent(title = { Text("title") }, content = { Text("free") })
            }
        }
        compose.onNodeWithText("title").assertIsDisplayed()
        compose.onNodeWithText("free").assertIsDisplayed()
    }

    @Test
    fun iconAndTitleWithoutContent() {
        compose.setContent {
            MaterialTheme {
                AlertDialogContent(icon = { Text("icon") }, title = { Text("title") })
            }
        }
        compose.onNodeWithText("icon").assertIsDisplayed()
        compose.onNodeWithText("title").assertIsDisplayed()
    }
}
