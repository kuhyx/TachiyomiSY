package eu.kanade.presentation.webview

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.kevinnzou.web.WebViewNavigator
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.components.WarningBanner
import eu.kanade.tachiyomi.R
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
