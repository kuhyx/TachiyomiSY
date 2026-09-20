package tachiyomi.presentation.core.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class LazyColumnWithActionTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun listAndEnabledActionByDefault() {
        var clicks = 0
        compose.setContent {
            MaterialTheme {
                LazyColumnWithAction(
                    contentPadding = PaddingValues(8.dp),
                    actionLabel = "Apply",
                    onClickAction = { clicks++ },
                ) {
                    items(3) { index -> Text("Row $index") }
                }
            }
        }
        compose.onNodeWithText("Row 0").assertIsDisplayed()
        compose.onNodeWithText("Row 2").assertIsDisplayed()
        compose.onNodeWithText("Apply").performClick()
        compose.runOnIdle { clicks shouldBe 1 }
    }

    @Test
    fun disabledActionWithModifier() {
        compose.setContent {
            MaterialTheme {
                LazyColumnWithAction(
                    contentPadding = PaddingValues(0.dp),
                    actionLabel = "Apply",
                    onClickAction = {},
                    modifier = Modifier.testTag("column"),
                    actionEnabled = false,
                ) {
                    item { Text("Only row") }
                }
            }
        }
        compose.onNodeWithTag("column").assertExists()
        compose.onNodeWithText("Only row").assertIsDisplayed()
        compose.onNodeWithText("Apply").assertIsNotEnabled()
    }
}
