package tachiyomi.presentation.core.components.material

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
internal class ScaffoldTest {
    @get:Rule
    val compose = createComposeRule()

    private var padding = PaddingValues(0.dp)
    private var contentColor = Color.Unspecified
    private var onBackground = Color.Unspecified

    @Test
    fun defaultsAreTheBackground() {
        compose.setContent {
            MaterialTheme {
                onBackground = MaterialTheme.colorScheme.onBackground
                Scaffold { inner ->
                    padding = inner
                    contentColor = LocalContentColor.current
                    Text("body", Modifier.padding(inner))
                }
            }
        }
        compose.onNodeWithText("body").assertIsDisplayed()
        contentColor shouldBe onBackground
        padding.calculateTopPadding() shouldBe 0.dp
        padding.calculateBottomPadding() shouldBe 0.dp
    }

    @Test
    fun everyParameterGiven() {
        compose.setContent {
            MaterialTheme {
                Scaffold(
                    modifier = Modifier.testTag("scaffold"),
                    topBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState()),
                    topBar = { behavior -> TopAppBar(title = { Text("top") }, scrollBehavior = behavior) },
                    bottomBar = { Box(Modifier.fillMaxWidth().height(50.dp).testTag("bottom")) },
                    startBar = { Box(Modifier.size(30.dp).testTag("start")) },
                    snackbarHost = { Text("snack") },
                    floatingActionButton = { Box(Modifier.size(56.dp).testTag("fab")) },
                    floatingActionButtonPosition = FabPosition.Center,
                    containerColor = Color.Red,
                    contentColor = Color.White,
                    contentWindowInsets = WindowInsets(left = 4.dp, top = 6.dp, right = 8.dp, bottom = 10.dp),
                ) { inner ->
                    padding = inner
                    contentColor = LocalContentColor.current
                    Text("body", Modifier.padding(inner))
                }
            }
        }
        val height = compose.onRoot().getBoundsInRoot().height
        compose.onNodeWithTag("scaffold").assertIsDisplayed()
        compose.onNodeWithText("top").assertIsDisplayed()
        compose.onNodeWithText("snack").assertIsDisplayed()
        compose.onNodeWithTag("bottom").assertTopPositionInRootIsEqualTo(height - 50.dp)
        compose.onNodeWithTag("fab").assertTopPositionInRootIsEqualTo(height - 50.dp - 56.dp - 16.dp)
        compose.onNodeWithText("body").assertIsDisplayed()
        contentColor shouldBe Color.White
        padding.calculateBottomPadding() shouldBe 50.dp + 56.dp + 16.dp
    }

    @Test
    fun consumedInsetsAreExcluded() {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.consumeWindowInsets(WindowInsets(top = 6.dp, bottom = 4.dp))) {
                    Scaffold(
                        contentWindowInsets = WindowInsets(top = 6.dp, bottom = 10.dp),
                    ) { inner ->
                        padding = inner
                        Text("body", Modifier.padding(inner))
                    }
                }
            }
        }
        compose.onNodeWithText("body").assertIsDisplayed()
        padding.calculateTopPadding() shouldBe 0.dp
        padding.calculateBottomPadding() shouldBe 6.dp
    }
}
