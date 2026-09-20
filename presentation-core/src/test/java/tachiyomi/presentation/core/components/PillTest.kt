package tachiyomi.presentation.core.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.sp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class PillTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun styledPillWithDefaults() {
        compose.setContent { MaterialTheme { Pill(text = "one", style = null) } }
        compose.onNodeWithText("one").assertIsDisplayed()
    }

    @Test
    fun styledPillWithEveryOption() {
        compose.setContent {
            MaterialTheme {
                Pill(
                    text = "two",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.testTag("pill"),
                    color = Color.Red,
                    contentColor = Color.Blue,
                )
            }
        }
        compose.onNodeWithTag("pill").assertExists()
        compose.onNodeWithText("two").assertIsDisplayed()
    }

    @Test
    fun sizedPillWithDefaults() {
        compose.setContent { MaterialTheme { Pill(text = "three") } }
        compose.onNodeWithText("three").assertIsDisplayed()
    }

    @Test
    fun sizedPillWithEveryOption() {
        compose.setContent {
            MaterialTheme {
                Pill(
                    text = "four",
                    modifier = Modifier.testTag("pill"),
                    color = Color.Green,
                    contentColor = Color.Black,
                    fontSize = 20.sp,
                )
            }
        }
        compose.onNodeWithTag("pill").assertExists()
        compose.onNodeWithText("four").assertIsDisplayed()
    }
}
