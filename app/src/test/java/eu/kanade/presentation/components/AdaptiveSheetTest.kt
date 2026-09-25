package eu.kanade.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.NavigatorDisposeBehavior
import eu.kanade.presentation.util.Screen
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class AdaptiveSheetTest {
    @get:Rule
    val compose = createComposeRule()

    private class Label(private val text: String) : Screen() {
        @Composable
        override fun Content() {
            Text(text)
        }
    }

    @Test
    fun sheetShowsItsContent() {
        compose.setContent {
            MaterialTheme {
                AdaptiveSheet(onDismissRequest = {}) { Text("sheet") }
            }
        }
        compose.onNodeWithText("sheet").assertExists()
    }

    @Test
    fun sheetWithExplicitArguments() {
        compose.setContent {
            MaterialTheme {
                AdaptiveSheet(onDismissRequest = {}, modifier = Modifier, enableSwipeDismiss = false) { Text("fixed") }
            }
        }
        compose.onNodeWithText("fixed").assertExists()
    }

    @Test
    fun navigatorSheetShowsTheScreen() {
        compose.setContent {
            MaterialTheme { NavigatorAdaptiveSheet(screen = Label("top level"), onDismissRequest = {}) }
        }
        compose.onNodeWithText("top level").assertExists()
    }

    @Test
    fun nestedSheetDisposesItsScreens() {
        var shown by mutableStateOf(true)
        var swipe: Boolean? = null
        compose.setContent {
            MaterialTheme {
                Navigator(
                    screen = Label("outer"),
                    disposeBehavior = NavigatorDisposeBehavior(disposeNestedNavigators = false),
                ) {
                    if (shown) {
                        NavigatorAdaptiveSheet(
                            screen = Label("nested"),
                            enableSwipeDismiss = { navigator ->
                                (navigator.size == 1).also { swipe = it }
                            },
                            onDismissRequest = {},
                        )
                    }
                }
            }
        }
        compose.onNodeWithText("nested").assertExists()
        swipe shouldBe true
        shown = false
        compose.waitForIdle()
        compose.onNodeWithText("nested").assertDoesNotExist()
    }
}
