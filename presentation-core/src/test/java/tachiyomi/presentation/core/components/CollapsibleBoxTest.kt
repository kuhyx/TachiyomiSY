package tachiyomi.presentation.core.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val HEADING = "Section"
private const val BODY = "Section body"

@RunWith(RobolectricTestRunner::class)
internal class CollapsibleBoxTest {
    @get:Rule
    val compose = createComposeRule()

    private var tick by mutableIntStateOf(0)

    @Test
    fun startsCollapsed() {
        compose.setContent { MaterialTheme { CollapsibleBox(heading = HEADING) { Text(text = BODY) } } }
        compose.onNodeWithText(HEADING).assertIsDisplayed()
        compose.onNodeWithText(BODY).assertDoesNotExist()
    }

    @Test
    fun startsExpandedWhenAsked() {
        compose.setContent {
            MaterialTheme { CollapsibleBox(heading = HEADING, startExpanded = true) { Text(text = BODY) } }
        }
        compose.onNodeWithText(BODY).assertIsDisplayed()
    }

    @Test
    fun headingTapTogglesContent() {
        compose.setContent { MaterialTheme { CollapsibleBox(heading = HEADING) { Text(text = BODY) } } }
        compose.onNodeWithText(HEADING).performClick()
        compose.waitForIdle()
        compose.onNodeWithText(BODY).assertIsDisplayed()
        compose.onNodeWithText(HEADING).performClick()
        compose.waitForIdle()
        compose.onNodeWithText(BODY).assertDoesNotExist()
    }

    @Test
    fun unchangedRecomposeIsInert() {
        compose.setContent {
            MaterialTheme {
                Column {
                    Text(text = "tick $tick")
                    CollapsibleBox(heading = HEADING, startExpanded = true) { Text(text = BODY) }
                }
            }
        }
        compose.runOnIdle { tick++ }
        compose.waitForIdle()
        compose.onNodeWithText("tick 1").assertIsDisplayed()
        compose.onNodeWithText(BODY).assertIsDisplayed()
    }
}
