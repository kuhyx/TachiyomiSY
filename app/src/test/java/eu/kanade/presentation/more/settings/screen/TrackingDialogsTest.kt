package eu.kanade.presentation.more.settings.screen

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import eu.kanade.tachiyomi.data.track.Tracker
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.verify
import kotlinx.coroutines.delay
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.i18n.MR

@RunWith(RobolectricTestRunner::class)
internal class TrackingDialogsTest {
    @get:Rule
    val compose = createComposeRule()

    private val tracker = stubTracker<Tracker>("Kitsu")
    private var dismissed = 0

    private fun count(text: String): Int = compose.onAllNodesWithText(text).fetchSemanticsNodes().size

    private fun login() {
        compose.setContent {
            MaterialTheme {
                TrackingLoginDialog(tracker = tracker, uNameStringRes = MR.strings.username) { dismissed++ }
            }
        }
        compose.onNodeWithText("Login").assertIsNotEnabled()
        compose.onNode(hasSetTextAction() and hasText("Username")).performTextReplacement("me")
        compose.onNode(hasSetTextAction() and hasText("Password")).performTextReplacement("secret")
        compose.onNodeWithText("Login").performClick()
    }

    @Test
    fun successfulLoginDismisses() {
        coEvery { tracker.login("me", "secret") } coAnswers { delay(500) }
        login()
        compose.awaitMain(timeoutMillis = 10_000) { count("Logging in…") == 1 }
        compose.awaitMain(timeoutMillis = 10_000) { dismissed == 1 }
        dismissed shouldBe 1
    }

    @Test
    fun failedLoginLogsOut() {
        coEvery { tracker.login(any(), any()) } throws IllegalStateException("bad credentials")
        login()
        verify(timeout = 10_000) { tracker.logout() }
        compose.awaitMain(timeoutMillis = 10_000) { count("Login") == 1 }
        dismissed shouldBe 0
    }

    @Test
    fun retryAfterFailureHidesError() {
        coEvery { tracker.login(any(), any()) } throws IllegalStateException("bad credentials") coAndThen {
            delay(500)
        }
        login()
        compose.awaitMain(timeoutMillis = 10_000) { count("Login") == 1 }
        compose.onNodeWithText("Login").performClick()
        compose.awaitMain(timeoutMillis = 10_000) { count("Logging in…") == 1 }
        compose.awaitMain(timeoutMillis = 10_000) { dismissed == 1 }
    }

    @Test
    fun closeIconDismisses() {
        compose.setContent {
            MaterialTheme { TrackingLoginDialog(tracker, MR.strings.email) { dismissed++ } }
        }
        compose.onNodeWithContentDescription("Close").performClick()
        dismissed shouldBe 1
    }

    @Test
    fun passwordVisibilityToggles() {
        val password = TextFieldState("x")
        compose.setContent {
            MaterialTheme { PasswordField(password = password, isError = true) }
        }
        compose.onNode(hasClickAction() and hasAnyAncestor(hasSetTextAction())).performClick()
        compose.waitForIdle()
        compose.onNode(hasClickAction() and hasAnyAncestor(hasSetTextAction())).performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Password").assertExists()
    }

    @Test
    fun logoutConfirms() {
        compose.setContent {
            MaterialTheme { TrackingLogoutDialog(tracker = tracker) { dismissed++ } }
        }
        compose.onNodeWithText("Log out").performClick()
        verify { tracker.logout() }
        dismissed shouldBe 1
    }

    @Test
    fun logoutCancels() {
        compose.setContent {
            MaterialTheme { TrackingLogoutDialog(tracker = tracker) { dismissed++ } }
        }
        compose.onNodeWithText("Cancel").performClick()
        verify(exactly = 0) { tracker.logout() }
        dismissed shouldBe 1
    }
}
