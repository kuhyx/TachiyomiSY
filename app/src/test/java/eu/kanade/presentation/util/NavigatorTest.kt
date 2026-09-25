package eu.kanade.presentation.util

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.OnBackPressedDispatcher
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.ScreenModelStore
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk
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

    @Test
    fun ioScopeIsCachedPerModel() {
        val model = object : ScreenModel {}
        val scope = model.ioCoroutineScope
        model.ioCoroutineScope shouldBe scope
        ScreenModelStore.remove(model)
        scope.isActive shouldBe false
    }

    @Test
    fun defaultTransitionPushesAndPops() {
        var navigator: Navigator? = null
        var dispatcher: OnBackPressedDispatcher? = null
        compose.setContent {
            dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
            Navigator(Page("first")) {
                navigator = it
                DefaultScreenTransition(navigator = it)
            }
        }
        compose.onNodeWithText("first").assertExists()
        compose.runOnIdle { navigator?.push(Page("second")) }
        compose.onNodeWithText("second").assertExists()
        compose.runOnIdle { dispatcher?.onBackPressed() }
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
