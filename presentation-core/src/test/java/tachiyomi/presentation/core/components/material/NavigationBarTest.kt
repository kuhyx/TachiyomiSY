package tachiyomi.presentation.core.components.material

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.LocalAbsoluteTonalElevation
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class NavigationBarTest {
    @get:Rule
    val compose = createComposeRule()

    private var contentColor = Color.Unspecified
    private var expectedContentColor = Color.Unspecified
    private var elevation: Dp = Dp.Unspecified

    @Test
    fun defaultsAreMaterials() {
        compose.setContent {
            MaterialTheme {
                expectedContentColor = MaterialTheme.colorScheme.contentColorFor(NavigationBarDefaults.containerColor)
                NavigationBar(modifier = Modifier.testTag("bar")) {
                    contentColor = LocalContentColor.current
                    elevation = LocalAbsoluteTonalElevation.current
                    NavigationBarItem(selected = true, onClick = {}, icon = { Text("home") })
                }
            }
        }
        compose.onNodeWithTag("bar").assertHeightIsEqualTo(80.dp)
        compose.onNodeWithText("home").assertIsDisplayed()
        contentColor shouldBe expectedContentColor
        elevation shouldBe NavigationBarDefaults.Elevation
    }

    @Test
    fun everyParameterGiven() {
        compose.setContent {
            MaterialTheme {
                NavigationBar(
                    modifier = Modifier.testTag("bar"),
                    containerColor = Color.Red,
                    contentColor = Color.White,
                    tonalElevation = 6.dp,
                    windowInsets = WindowInsets(bottom = 10.dp),
                ) {
                    contentColor = LocalContentColor.current
                    elevation = LocalAbsoluteTonalElevation.current
                    NavigationBarItem(selected = false, onClick = {}, icon = { Text("more") })
                }
            }
        }
        compose.onNodeWithTag("bar").assertHeightIsEqualTo(90.dp)
        compose.onNodeWithText("more").assertIsDisplayed()
        contentColor shouldBe Color.White
        elevation shouldBe 6.dp
    }

    @Test
    fun withoutModifier() {
        compose.setContent {
            MaterialTheme {
                NavigationBar { NavigationBarItem(selected = true, onClick = {}, icon = { Text("only") }) }
            }
        }
        compose.onNodeWithText("only").assertIsDisplayed()
    }
}
