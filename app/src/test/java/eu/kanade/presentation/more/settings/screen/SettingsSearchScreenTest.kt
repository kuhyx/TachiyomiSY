package eu.kanade.presentation.more.settings.screen

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performImeAction
import cafe.adriel.voyager.navigator.LocalNavigator
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class SettingsSearchScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val koin = SettingsKoin()
    private val harness = SettingsHarness(compose)

    @Before
    fun setUp() {
        stubTextureLimits()
        koin.start(*allScreensModules())
    }

    @After
    fun tearDown() {
        SearchableSettings.highlightKey = null
        unmockkAll()
        koin.stop()
    }

    private fun show(canPop: Boolean) {
        every { harness.navigator.canPop } returns canPop
        compose.setContent {
            CompositionLocalProvider(LocalNavigator provides harness.navigator) {
                MaterialTheme { SettingsSearchScreen().Content() }
            }
        }
        compose.waitForIdle()
    }

    private fun type(text: String) {
        compose.onNode(hasSetTextAction()).performTextReplacement(text)
        compose.waitForIdle()
    }

    private fun count(text: String) = compose.onAllNodesWithText(text).fetchSemanticsNodes().size

    @Test
    fun resultOpensScreen() {
        show(canPop = true)
        count("Search settings") shouldBe 1
        type("Pure black")
        compose.waitUntil(timeoutMillis = 10_000) { count("Pure black dark mode") == 1 }
        compose.onNodeWithText("Pure black dark mode").performClick()
        SearchableSettings.highlightKey shouldBe "Pure black dark mode"
        verify { harness.navigator.replace(SettingsAppearanceScreen) }
    }

    @Test
    fun noResultsAndClear() {
        show(canPop = false)
        type("zzzzqqq")
        compose.waitUntil(timeoutMillis = 10_000) { count("No results found") == 1 }
        compose.onNode(hasSetTextAction()).performImeAction()
        type("")
        count("Search settings") shouldBe 1
    }

    @Test
    fun topBarClearsAndPops() {
        val state = TextFieldState("abc")
        every { harness.navigator.canPop } returns true
        compose.setContent {
            CompositionLocalProvider(LocalNavigator provides harness.navigator) {
                MaterialTheme { SearchTopBar(state, FocusRequester()) }
            }
        }
        compose.onNodeWithContentDescription("Navigate up").performClick()
        verify { harness.navigator.pop() }
        compose.onNode(hasClickAction() and hasSetTextAction().not() and hasContentDescription("Navigate up").not())
            .performClick()
        compose.waitForIdle()
        state.text.toString() shouldBe ""
    }
}
