package eu.kanade.presentation.webview

import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.os.Message
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.stack.SnapshotStateStack
import cafe.adriel.voyager.core.stack.mutableStateStackOf
import com.kevinnzou.web.AccompanistWebChromeClient
import com.kevinnzou.web.AccompanistWebViewClient
import com.kevinnzou.web.WebContent
import com.kevinnzou.web.WebView
import com.kevinnzou.web.WebViewNavigator
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.components.WarningBanner
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.system.getHtml
import eu.kanade.tachiyomi.util.system.setDefaultSettings
import kotlinx.coroutines.launch
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun WebViewScreenContent(
    onNavigateUp: () -> Unit,
    initialTitle: String?,
    url: String,
    onShare: (String) -> Unit,
    onOpenInBrowser: (String) -> Unit,
    onClearCookies: (String) -> Unit,
    headers: Map<String, String> = emptyMap(),
    onUrlChange: (String) -> Unit = {},
) {
    val session = rememberWebViewSession(url, headers, onUrlChange)
    val currentWindow = session.windowStack.lastItemOrNull!!

    val popState = remember<() -> Unit> {
        {
            if (session.windowStack.size == 1) {
                onNavigateUp()
            } else {
                session.windowStack.pop()
            }
        }
    }

    BackHandler(session.windowStack.size > 1, popState)

    Scaffold(
        topBar = {
            Box {
                Column {
                    AppBar(
                        title = currentWindow.state.pageTitle ?: initialTitle,
                        subtitle = session.currentUrl,
                        navigateUp = onNavigateUp,
                        navigationIcon = Icons.Outlined.Close,
                        actions = {
                            AppBarActions(
                                webViewActions(
                                    navigator = currentWindow.navigator,
                                    onCloseTab = popState.takeIf { session.windowStack.size > 1 },
                                    onShare = { onShare(session.currentUrl) },
                                    onOpenInBrowser = { onOpenInBrowser(session.currentUrl) },
                                    onClearCookies = { onClearCookies(session.currentUrl) },
                                ),
                            )
                        },
                    )

                    if (session.showCloudflareHelp) {
                        CloudflareHelpBanner()
                    }
                }
                WebViewLoadingIndicator(currentWindow.state.loadingState)
            }
        },
    ) { contentPadding ->
        // We need to key the WebView composable to the window object since simply updating the WebView composable will
        // not cause it to re-invoke the WebView factory and render the new current window's WebView. This lets us
        // completely reset the WebView composable when the current window switches.
        key(currentWindow) {
            WindowWebView(
                currentWindow = currentWindow,
                windowStack = session.windowStack.items,
                headers = headers,
                client = session.webClient,
                chromeClient = session.webChromeClient,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            )
        }
    }
}

// The window stack, the two WebView clients and the observable bits they update.
private class WebViewSession(
    val windowStack: SnapshotStateStack<WebViewWindow>,
    val webClient: AccompanistWebViewClient,
    val webChromeClient: AccompanistWebChromeClient,
) {
    var currentUrl by mutableStateOf("")
    var showCloudflareHelp by mutableStateOf(false)
    var isActive = true
}

@Composable
private fun rememberWebViewSession(
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
private fun webViewClient(
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
private fun webChromeClient(
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

// Back/forward plus the overflow entries; "close tab" is prepended only while a popup window is open.
@Composable
private fun webViewActions(
    navigator: WebViewNavigator,
    onCloseTab: (() -> Unit)?,
    onShare: () -> Unit,
    onOpenInBrowser: () -> Unit,
    onClearCookies: () -> Unit,
): List<AppBar.AppBarAction> {
    val closeTab = onCloseTab?.let {
        AppBar.Action(
            title = stringResource(MR.strings.action_webview_close_tab),
            icon = ImageVector.vectorResource(R.drawable.ic_tab_close_24px),
            onClick = it,
        )
    }
    return listOfNotNull(
        closeTab,
        AppBar.Action(
            title = stringResource(MR.strings.action_webview_back),
            icon = Icons.AutoMirrored.Outlined.ArrowBack,
            onClick = {
                if (navigator.canGoBack) {
                    navigator.navigateBack()
                }
            },
            enabled = navigator.canGoBack,
        ),
        AppBar.Action(
            title = stringResource(MR.strings.action_webview_forward),
            icon = Icons.AutoMirrored.Outlined.ArrowForward,
            onClick = {
                if (navigator.canGoForward) {
                    navigator.navigateForward()
                }
            },
            enabled = navigator.canGoForward,
        ),
        AppBar.OverflowAction(
            title = stringResource(MR.strings.action_webview_refresh),
            onClick = { navigator.reload() },
        ),
        AppBar.OverflowAction(
            title = stringResource(MR.strings.action_share),
            onClick = onShare,
        ),
        AppBar.OverflowAction(
            title = stringResource(MR.strings.action_open_in_browser),
            onClick = onOpenInBrowser,
        ),
        AppBar.OverflowAction(
            title = stringResource(MR.strings.pref_clear_cookies),
            onClick = onClearCookies,
        ),
    )
}

@Composable
private fun CloudflareHelpBanner() {
    val uriHandler = LocalUriHandler.current
    Surface(
        modifier = Modifier.padding(8.dp),
    ) {
        WarningBanner(
            textRes = MR.strings.information_cloudflare_help,
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .clickable {
                    uriHandler.openUri(
                        "https://mihon.app/docs/guides/troubleshooting/#cloudflare",
                    )
                },
        )
    }
}

// The current window's WebView; popup windows adopt the WebView Android hands them via WebViewTransport.
@Composable
private fun WindowWebView(
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

private fun initializePopup(webView: WebView, message: Message): WebView {
    val transport = message.obj as WebView.WebViewTransport
    transport.webView = webView
    message.sendToTarget()
    return webView
}
