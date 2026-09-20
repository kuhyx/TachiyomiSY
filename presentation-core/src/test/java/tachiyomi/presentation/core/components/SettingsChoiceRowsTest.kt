package tachiyomi.presentation.core.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.i18n.MR

@RunWith(RobolectricTestRunner::class)
internal class SettingsChoiceRowsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun textItemForwardsTypedText() {
        var value by mutableStateOf("")
        compose.setContent {
            MaterialTheme { TextItem(label = "Name", value = value, onChange = { typed -> value = typed }) }
        }
        compose.onNodeWithText("Name").assertIsDisplayed()
        compose.onNode(hasSetTextAction()).performTextInput("abc")
        compose.runOnIdle { value shouldBe "abc" }
        compose.onNodeWithText("abc").assertIsDisplayed()
    }

    @Test
    fun chipRowShowsHeadingAndChips() {
        compose.setContent {
            MaterialTheme {
                SettingsChipRow(MR.strings.action_cancel) {
                    Text("chip one")
                    Text("chip two")
                }
            }
        }
        compose.onNodeWithText("Cancel").assertIsDisplayed()
        compose.onNodeWithText("chip one").assertIsDisplayed()
        compose.onNodeWithText("chip two").assertIsDisplayed()
    }

    @Test
    fun iconGridShowsHeadingAndCells() {
        compose.setContent {
            MaterialTheme {
                SettingsIconGrid(MR.strings.action_ok) {
                    item { Text("cell one") }
                    item { Text("cell two") }
                }
            }
        }
        compose.onNodeWithText("OK").assertIsDisplayed()
        compose.onNodeWithText("cell one").assertIsDisplayed()
        compose.onNodeWithText("cell two").assertIsDisplayed()
    }
}
