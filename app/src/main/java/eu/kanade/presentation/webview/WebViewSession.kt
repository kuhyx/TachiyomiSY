package eu.kanade.presentation.webview

import android.graphics.Bitmap
import android.os.Message
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.stack.SnapshotStateStack
import cafe.adriel.voyager.core.stack.mutableStateStackOf
import com.kevinnzou.web.AccompanistWebChromeClient
import com.kevinnzou.web.AccompanistWebViewClient
import com.kevinnzou.web.WebContent
import com.kevinnzou.web.WebView
import com.kevinnzou.web.WebViewNavigator
import eu.kanade.tachiyomi.util.system.getHtml
import kotlinx.coroutines.launch

// The window stack, the two WebView clients and the observable bits they update.
internal class WebViewSession(
    val windowStack: SnapshotStateStack<WebViewWindow>,
    val webClient: AccompanistWebViewClient,
    val webChromeClient: AccompanistWebChromeClient,
) {
    var currentUrl by mutableStateOf("")
    var showCloudflareHelp by mutableStateOf(false)
    var isActive = true
}

@Composable
internal fun rememberWebViewSession(
    url: String,
    headers: Map<String, String>,
    onUrlChange: (String) -> Unit,
): WebViewSession {
    val coroutineScope = rememberCoroutineScope()
    val session = remember {
        lateinit var session: WebViewSession
        val windowStack = mutableStateStackOf(
            WebViewWindow(
                WebContent.Url(url = url, additionalHttpHeaders = headers),
                WebViewNavigator(coroutineScope),
            ),
        )
        session = WebViewSession(
            windowStack = windowStack,
            webClient = webViewClient(
                headers = headers,
                onUrlChanged = {
                    session.currentUrl = it
                    onUrlChange(it)
                },
                onPageFinished = { view ->
                    coroutineScope.launch {
                        val html = view.getHtml()
                        session.showCloudflareHelp = "window._cf_chl_opt" in html || "Ray ID is" in html
                    }
                },
            ),
            webChromeClient = webChromeClient(
                isActive = { session.isActive },
                onNewWindow = { windowStack.push(WebViewWindow(it, WebViewNavigator(coroutineScope))) },
            ),
        )
        session.currentUrl = url
        session
    }
    DisposableEffect(Unit) {
        onDispose { session.isActive = false }
    }
    return session
}

// Tracks the visible URL and hands finished pages back (used to detect Cloudflare challenges).
internal fun webViewClient(
    headers: Map<String, String>,
    onUrlChanged: (String) -> Unit,
    onPageFinished: (WebView) -> Unit,
): AccompanistWebViewClient = object : AccompanistWebViewClient() {
    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        url?.let(onUrlChanged)
    }

    override fun onPageFinished(view: WebView, url: String?) {
        super.onPageFinished(view, url)
        onPageFinished(view)
    }

    override fun doUpdateVisitedHistory(
        view: WebView,
        url: String?,
        isReload: Boolean,
    ) {
        super.doUpdateVisitedHistory(view, url, isReload)
        url?.let(onUrlChanged)
    }

    override fun shouldOverrideUrlLoading(
        view: WebView?,
        request: WebResourceRequest?,
    ): Boolean {
        val url = request?.url?.toString() ?: return false

        // Ignore intents urls
        if (url.startsWith("intent://")) return true

        // Only open valid web urls
        if ((url.startsWith("http") || url.startsWith("https")) && url != view?.url) {
            view?.loadUrl(url, headers)
            return true
        }

        return false
    }
}

// Opens user-initiated popups as new windows and auto-dismisses JS dialogs once the screen is gone.
internal fun webChromeClient(
    isActive: () -> Boolean,
    onNewWindow: (Message) -> Unit,
): AccompanistWebChromeClient = object : AccompanistWebChromeClient() {
    override fun onCreateWindow(
        view: WebView,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message,
    ): Boolean {
        // if it wasn't initiated by a user gesture, we should ignore it like a normal browser would
        if (isUserGesture) {
            onNewWindow(resultMsg)
            return true
        }
        return false
    }

    override fun onJsAlert(view: WebView, url: String?, message: String?, result: JsResult): Boolean {
        if (!isActive()) {
            result.confirm()
            return true
        }
        return super.onJsAlert(view, url, message, result)
    }

    override fun onJsConfirm(view: WebView, url: String?, message: String?, result: JsResult): Boolean {
        if (!isActive()) {
            result.cancel()
            return true
        }
        return super.onJsConfirm(view, url, message, result)
    }

    override fun onJsPrompt(
        view: WebView,
        url: String?,
        message: String?,
        defaultValue: String?,
        result: JsPromptResult,
    ): Boolean {
        if (!isActive()) {
            result.cancel()
            return true
        }
        return super.onJsPrompt(view, url, message, defaultValue, result)
    }
}
