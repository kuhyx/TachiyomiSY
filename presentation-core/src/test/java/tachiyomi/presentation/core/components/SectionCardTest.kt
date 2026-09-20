package tachiyomi.presentation.core.components

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.i18n.MR

@RunWith(RobolectricTestRunner::class)
internal class SectionCardTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun cardWithoutTitle() {
        compose.setContent {
            MaterialTheme {
                LazyColumn {
                    item { SectionCard { Text("Body") } }
                }
            }
        }
        compose.onNodeWithText("Body").assertIsDisplayed()
        compose.onNodeWithText("Cancel").assertDoesNotExist()
    }

    @Test
    fun cardWithTitle() {
        compose.setContent {
            MaterialTheme {
                LazyColumn {
                    item { SectionCard(titleRes = MR.strings.action_cancel) { Text("Body") } }
                }
            }
        }
        compose.onNodeWithText("Cancel").assertIsDisplayed()
        compose.onNodeWithText("Body").assertIsDisplayed()
    }
}
