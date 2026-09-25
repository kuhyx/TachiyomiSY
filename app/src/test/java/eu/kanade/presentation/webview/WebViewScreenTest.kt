package eu.kanade.presentation.webview

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.kevinnzou.web.AccompanistWebChromeClient
import com.kevinnzou.web.AccompanistWebViewClient
import eu.kanade.presentation.util.ProvideBack
import eu.kanade.presentation.util.TestBackOwner
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
internal class WebViewScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()
    private val back = TestBackOwner()

    private fun show(headers: Map<String, String>? = null) {
        compose.setContent {
            ProvideBack(back) {
                MaterialTheme {
                    if (headers == null) {
                        WebViewScreenContent(
                            onNavigateUp = { events += "up" },
                            initialTitle = "Initial",
                            url = "https://example.com/",
                            onShare = { events += "share $it" },
                            onOpenInBrowser = { events += "browser $it" },
                            onClearCookies = { events += "cookies $it" },
                        )
                    } else {
                        WebViewScreenContent(
                            onNavigateUp = {},
                            initialTitle = null,
                            url = "https://example.com/",
                            onShare = {},
                            onOpenInBrowser = {},
                            onClearCookies = {},
                            headers = headers,
                            onUrlChange = { events += "url $it" },
                        )
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun webView(): WebView = compose.webViews().single()

    private fun client() = shadowOf(webView()).webViewClient as AccompanistWebViewClient

    private fun chrome() = shadowOf(webView()).webChromeClient as AccompanistWebChromeClient

    @Test
    fun overflowUsesCurrentUrl() {
        show()
        compose.onNodeWithText("Initial").assertExists()
        listOf("Refresh", "Share", "Open in browser", "Clear cookies").forEach {
            compose.onNodeWithContentDescription("More options").performClick()
            compose.onNodeWithText(it).performClick()
        }
        compose.onNodeWithContentDescription("Navigate up").performClick()
        events shouldContainExactly listOf(
            "share https://example.com/",
            "browser https://example.com/",
            "cookies https://example.com/",
            "up",
        )
    }

    @Test
    fun pageEventsUpdateTheUrl() {
        show(headers = mapOf("user-agent" to "Agent"))
        webView().settings.userAgentString shouldBe "Agent"
        compose.runOnIdle {
            client().onPageStarted(webView(), "https://a.example/", null)
            client().onPageStarted(webView(), null, null)
            client().doUpdateVisitedHistory(webView(), "https://b.example/", false)
            client().doUpdateVisitedHistory(webView(), null, false)
        }
        compose.onNodeWithText("https://b.example/").assertExists()
        events shouldContainExactly listOf("url https://a.example/", "url https://b.example/")
    }

    @Test
    fun cloudflareShowsHelpBanner() {
        show()
        compose.runOnIdle { client().onPageFinished(webView(), "https://example.com/") }
        compose.runOnIdle { shadowOf(webView()).lastEvaluatedJavascriptCallback.onReceiveValue("<p>plain</p>") }
        compose.onNodeWithText("Tap here for help with Cloudflare").assertDoesNotExist()
        compose.runOnIdle { client().onPageFinished(webView(), "https://example.com/") }
        compose.runOnIdle { shadowOf(webView()).lastEvaluatedJavascriptCallback.onReceiveValue("Ray ID is 1") }
        compose.onNodeWithText("Tap here for help with Cloudflare").performClick()
        compose.runOnIdle { client().onPageFinished(webView(), "https://example.com/") }
        compose.runOnIdle { shadowOf(webView()).lastEvaluatedJavascriptCallback.onReceiveValue("window._cf_chl_opt") }
        compose.onNodeWithText("Tap here for help with Cloudflare").assertExists()
    }

    @Test
    fun urlOverridesFollowWebLinksOnly() {
        show()
        compose.runOnIdle {
            val client = client()
            val view = webView()
            client.shouldOverrideUrlLoading(null, null) shouldBe false
            client.shouldOverrideUrlLoading(view, request(null)) shouldBe false
            client.shouldOverrideUrlLoading(view, request("intent://x")) shouldBe true
            client.shouldOverrideUrlLoading(view, request("ftp://x")) shouldBe false
            client.shouldOverrideUrlLoading(view, request("https://next.example/")) shouldBe true
            client.shouldOverrideUrlLoading(view, request(view.url)) shouldBe false
            client.shouldOverrideUrlLoading(null, request("https://other.example/")) shouldBe true
        }
    }

    @Test
    fun popupsOpenAndCloseAsTabs() {
        show()
        val firstView = webView()
        compose.runOnIdle {
            chrome().onCreateWindow(firstView, false, false, Message.obtain()) shouldBe false
            chrome().onCreateWindow(firstView, false, true, popupMessage(firstView)) shouldBe true
        }
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Close tab").performClick()
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Close tab").assertDoesNotExist()
        webView() shouldBe firstView
        compose.runOnIdle {
            chrome().onCreateWindow(firstView, false, true, popupMessage(firstView))
        }
        compose.waitForIdle()
        compose.runOnIdle { back.pressBack() }
        compose.onNodeWithContentDescription("Close tab").assertDoesNotExist()
    }

    private fun request(url: String?): WebResourceRequest {
        val request = mockk<WebResourceRequest>()
        every { request.url } returns url?.let(Uri::parse)
        return request
    }

    private fun popupMessage(view: WebView): Message = Message.obtain(Handler(Looper.getMainLooper())).also {
        it.obj = view.WebViewTransport()
    }
}
