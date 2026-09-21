package eu.kanade.presentation.webview

import android.content.pm.ApplicationInfo
import android.os.Message
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.kevinnzou.web.AccompanistWebChromeClient
import com.kevinnzou.web.AccompanistWebViewClient
import com.kevinnzou.web.WebContent
import com.kevinnzou.web.WebView
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.util.system.setDefaultSettings

// The current window's WebView; popup windows adopt the WebView Android hands them via WebViewTransport.
@Composable
internal fun WindowWebView(
    currentWindow: WebViewWindow,
    windowStack: List<WebViewWindow>,
    headers: Map<String, String>,
    client: AccompanistWebViewClient,
    chromeClient: AccompanistWebChromeClient,
    modifier: Modifier = Modifier,
) {
    WebView(
        state = currentWindow.state,
        modifier = modifier,
        navigator = currentWindow.navigator,
        onCreated = { webView ->
            webView.setDefaultSettings()

            // Debug mode (chrome://inspect/#devices)
            if (BuildConfig.DEBUG &&
                0 != webView.context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE
            ) {
                WebView.setWebContentsDebuggingEnabled(true)
            }

            headers["user-agent"]?.let {
                webView.settings.userAgentString = it
            }
        },
        onDispose = { webView ->
            val window = windowStack.find { it.webView == webView }
            if (window == null) {
                // If we couldn't find any window on the stack that owns this WebView, it means that we can
                // safely dispose of it because the window containing it has been closed.
                webView.destroy()
            } else {
                // The composable is being disposed but the WebView object is not.
                // When the WebView element is recomposed, we will want the WebView to resume from its state
                // before it was unmounted, we won't want it to reset back to its original target.
                window.state.content = WebContent.NavigatorOnly
            }
        },
        client = client,
        chromeClient = chromeClient,
        factory = { context ->
            currentWindow.webView
                ?: WebView(context).also { webView ->
                    currentWindow.webView = webView
                    currentWindow.popupMessage?.let { initializePopup(webView, it) }
                }
        },
    )
}

internal fun initializePopup(webView: WebView, message: Message): WebView {
    val transport = message.obj as WebView.WebViewTransport
    transport.webView = webView
    message.sendToTarget()
    return webView
}
