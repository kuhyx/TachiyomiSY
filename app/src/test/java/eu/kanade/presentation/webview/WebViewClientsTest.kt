package eu.kanade.presentation.webview

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Message
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.WebView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.kevinnzou.web.WebContent
import com.kevinnzou.web.WebViewNavigator
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.MainScope
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class WebViewClientsTest {
    @get:Rule
    val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun dialogsPassThroughWhileActive() {
        val chrome = webChromeClient(isActive = { true }, onNewWindow = {})
        val view = WebView(context)
        val result = mockk<JsResult>(relaxed = true)
        val prompt = mockk<JsPromptResult>(relaxed = true)
        chrome.onJsAlert(view, "u", "m", result) shouldBe false
        chrome.onJsConfirm(view, "u", "m", result) shouldBe false
        chrome.onJsPrompt(view, "u", "m", "d", prompt) shouldBe false
        verify(exactly = 0) { result.confirm() }
    }

    @Test
    fun dialogsAutoDismissOnceGone() {
        val chrome = webChromeClient(isActive = { false }, onNewWindow = {})
        val view = WebView(context)
        val result = mockk<JsResult>(relaxed = true)
        val prompt = mockk<JsPromptResult>(relaxed = true)
        chrome.onJsAlert(view, "u", "m", result) shouldBe true
        chrome.onJsConfirm(view, "u", "m", result) shouldBe true
        chrome.onJsPrompt(view, "u", "m", "d", prompt) shouldBe true
        verify { result.confirm() }
        verify { result.cancel() }
        verify { prompt.cancel() }
    }

    @Test
    fun sessionGoesInactiveOnDispose() {
        var shown by mutableStateOf(true)
        var session: WebViewSession? = null
        compose.setContent {
            if (shown) session = rememberWebViewSession(url = "https://example.com/", headers = emptyMap(), onUrlChange = {})
        }
        compose.waitForIdle()
        session?.isActive shouldBe true
        session?.currentUrl shouldBe "https://example.com/"
        shown = false
        compose.waitForIdle()
        session?.isActive shouldBe false
    }

    @Test
    fun popupWindowsKeepTheirMessage() {
        val message = Message.obtain()
        val window = WebViewWindow(message, WebViewNavigator(MainScope()))
        window.popupMessage shouldBe message
        window.state.content shouldBe WebContent.NavigatorOnly
        WebViewWindow(WebContent.NavigatorOnly, WebViewNavigator(MainScope())).popupMessage shouldBe null
    }

    @Test
    fun debuggableAppsEnableInspection() {
        val info = context.applicationInfo
        info.flags = info.flags or ApplicationInfo.FLAG_DEBUGGABLE
        compose.setContent {
            WebViewScreenContent(
                onNavigateUp = {},
                initialTitle = null,
                url = "https://example.com/",
                onShare = {},
                onOpenInBrowser = {},
                onClearCookies = {},
            )
        }
        compose.waitForIdle()
        compose.webViews().size shouldBe 1
    }

    @Test
    fun releaseFlagsSkipInspection() {
        val info = context.applicationInfo
        info.flags = info.flags and ApplicationInfo.FLAG_DEBUGGABLE.inv()
        compose.setContent {
            WebViewScreenContent(
                onNavigateUp = {},
                initialTitle = null,
                url = "https://example.com/",
                onShare = {},
                onOpenInBrowser = {},
                onClearCookies = {},
            )
        }
        compose.waitForIdle()
        compose.webViews().size shouldBe 1
    }
}
