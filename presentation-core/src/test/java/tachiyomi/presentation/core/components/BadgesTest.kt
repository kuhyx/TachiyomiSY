package tachiyomi.presentation.core.components

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class BadgesTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun groupAndTextBadgeWithDefaults() {
        compose.setContent {
            MaterialTheme {
                BadgeGroup {
                    Badge(text = "new")
                }
            }
        }
        compose.onNodeWithText("new").assertIsDisplayed()
    }

    @Test
    fun groupAndTextBadgeWithOptions() {
        compose.setContent {
            MaterialTheme {
                BadgeGroup(modifier = Modifier.testTag("group"), shape = CircleShape) {
                    Badge(
                        text = "old",
                        modifier = Modifier.testTag("badge"),
                        color = Color.Red,
                        textColor = Color.White,
                        shape = CircleShape,
                    )
                }
            }
        }
        compose.onNodeWithTag("group").assertExists()
        compose.onNodeWithTag("badge").assertExists()
        compose.onNodeWithText("old").assertIsDisplayed()
    }

    @Test
    fun iconBadgeWithDefaults() {
        compose.setContent { MaterialTheme { Badge(imageVector = Icons.Default.Star) } }
        compose.onRoot().assertExists()
    }

    @Test
    fun iconBadgeWithOptions() {
        compose.setContent {
            MaterialTheme {
                Badge(
                    imageVector = Icons.Default.Star,
                    modifier = Modifier.testTag("icon"),
                    color = Color.Blue,
                    iconColor = Color.Yellow,
                    shape = CircleShape,
                )
            }
        }
        compose.onNodeWithTag("icon").assertIsDisplayed()
    }
}
