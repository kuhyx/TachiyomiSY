package eu.kanade.presentation.util

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class NavigatorTest {
    @get:Rule
    val compose = createComposeRule()

    private class Page(private val label: String) : Screen() {
        @Composable
        override fun Content() {
            Text(label)
        }
    }

    private object PlainTab : Tab {
        override val options: TabOptions
            @Composable get() = TabOptions(index = 0u, title = "tab")

        @Composable
        override fun Content() {
            Text("tab content")
        }
    }

    @Test
    fun backPressIsUnsetByDefault() {
        var seen: (() -> Unit)? = {}
        compose.setContent { seen = LocalBackPress.current }
        compose.waitForIdle()
        seen.shouldBeNull()
    }

    @Test
    fun tabDefaultsAreEnabled() {
        var enabled = false
        compose.setContent { enabled = PlainTab.isEnabled() }
        compose.waitForIdle()
        enabled shouldBe true
        runBlocking { PlainTab.onReselect(mockk()) }
    }

    @Test
    fun screensGetUniqueKeys() {
        Page("a").key shouldNotBe Page("a").key
    }

    private class ModelScreen(private val onScope: (CoroutineScope) -> Unit) : Screen() {
        @Composable
        override fun Content() {
            val model = rememberScreenModel { object : ScreenModel {} }
            val scope = model.ioCoroutineScope
            model.ioCoroutineScope shouldBe scope
            onScope(scope)
            Text("model")
        }
    }

    @Test
    fun ioScopeEndsWithTheScreen() {
        var scope: CoroutineScope? = null
        var navigator: Navigator? = null
        compose.setContent {
            Navigator(ModelScreen { scope = it }) {
                navigator = it
                CurrentScreen()
            }
        }
        compose.onNodeWithText("model").assertExists()
        scope?.isActive shouldBe true
        compose.runOnIdle { navigator?.replaceAll(Page("other")) }
        compose.waitForIdle()
        scope?.isActive shouldBe false
    }

    @Test
    fun defaultTransitionPushesAndPops() {
        var navigator: Navigator? = null
        val back = TestBackOwner()
        compose.setContent {
            ProvideBack(back) {
                Navigator(Page("first")) {
                    navigator = it
                    DefaultScreenTransition(navigator = it)
                }
            }
        }
        compose.onNodeWithText("first").assertExists()
        compose.runOnIdle { navigator?.push(Page("second")) }
        compose.onNodeWithText("second").assertExists()
        compose.runOnIdle { back.pressBack() }
        compose.onNodeWithText("first").assertExists()
        navigator?.canPop shouldBe false
    }

    @Test
    fun customContentWrapsTheScreen() {
        compose.setContent {
            Navigator(Page("inner")) {
                ScreenTransition(
                    navigator = it,
                    transition = { EnterTransition.None togetherWith ExitTransition.None },
                    content = { screen -> Text("wrapped ${screen.key.isNotEmpty()}") },
                )
            }
        }
        compose.onNodeWithText("wrapped true").assertExists()
    }
}
