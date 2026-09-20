package tachiyomi.presentation.core.components.material

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRailDefaults
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class NavigationRailTest {
    @get:Rule
    val compose = createComposeRule()

    private var contentColor = Color.Unspecified
    private var expectedContentColor = Color.Unspecified

    @Test
    fun defaultsAreMaterials() {
        compose.setContent {
            MaterialTheme {
                expectedContentColor = MaterialTheme.colorScheme.contentColorFor(NavigationRailDefaults.ContainerColor)
                NavigationRail(modifier = Modifier.testTag("rail")) {
                    contentColor = LocalContentColor.current
                    NavigationRailItem(selected = true, onClick = {}, icon = { Text("home") })
                }
            }
        }
        compose.onNodeWithTag("rail").assertWidthIsAtLeast(80.dp)
        compose.onNodeWithText("home").assertIsDisplayed()
        compose.onNodeWithText("head").assertDoesNotExist()
        contentColor shouldBe expectedContentColor
    }

    @Test
    fun everyParameterGiven() {
        compose.setContent {
            MaterialTheme {
                NavigationRail(
                    modifier = Modifier.testTag("rail"),
                    containerColor = Color.Red,
                    contentColor = Color.White,
                    header = { Text("head") },
                    windowInsets = WindowInsets(left = 10.dp),
                ) {
                    contentColor = LocalContentColor.current
                    NavigationRailItem(selected = false, onClick = {}, icon = { Text("more") })
                }
            }
        }
        compose.onNodeWithTag("rail").assertWidthIsAtLeast(90.dp)
        compose.onNodeWithText("head").assertIsDisplayed()
        compose.onNodeWithText("more").assertIsDisplayed()
        contentColor shouldBe Color.White
    }

    @Test
    fun withoutModifier() {
        compose.setContent {
            MaterialTheme {
                NavigationRail { NavigationRailItem(selected = true, onClick = {}, icon = { Text("only") }) }
            }
        }
        compose.onNodeWithText("only").assertIsDisplayed()
    }
}
