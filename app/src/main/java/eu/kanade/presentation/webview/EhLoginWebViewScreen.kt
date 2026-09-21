package eu.kanade.presentation.webview

import android.content.pm.ApplicationInfo
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.kevinnzou.web.AccompanistWebViewClient
import com.kevinnzou.web.WebContent
import com.kevinnzou.web.WebView
import com.kevinnzou.web.WebViewNavigator
import com.kevinnzou.web.WebViewState
import com.kevinnzou.web.rememberWebViewNavigator
import com.kevinnzou.web.rememberWebViewState
import eu.kanade.presentation.components.AppBar
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.util.system.setDefaultSettings
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.components.material.Button
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource

private const val HALF = 0.5F
private const val ADVANCED_OPTIONS_WIDTH = 0.8F

@Composable
internal fun EhLoginWebViewScreen(
    onUp: () -> Unit,
    onPageFinished: (view: WebView, url: String) -> Unit,
    onClickRecheckLoginStatus: (loadUrl: (String) -> Unit) -> Unit,
    onClickAlternateLoginPage: (loadUrl: (String) -> Unit) -> Unit,
    onClickSkipPageRestyling: (loadUrl: (String) -> Unit) -> Unit,
    onClickCustomIgneousCookie: () -> Unit,
) {
    val state = rememberWebViewState(
        url = "https://forums.e-hentai.org/index.php?act=Login",
    )
    val navigator = rememberWebViewNavigator()
    val loading by produceState(true) {
        CookieManager.getInstance().removeAllCookies {
            value = false
        }
    }

    Scaffold(
        topBar = {
            Box {
                AppBar(
                    title = "ExHentai login",
                    navigateUp = onUp,
                    navigationIcon = Icons.Outlined.Close,
                )
                WebViewLoadingIndicator(state.loadingState, animated = true)
            }
        },
    ) { contentPadding ->

        if (!loading) {
            val webClient = remember {
                object : AccompanistWebViewClient() {
                    override fun onPageFinished(view: WebView, url: String?) {
                        super.onPageFinished(view, url)
                        onPageFinished(view, url ?: return)
                    }
                }
            }
            var showAdvancedOptions by rememberSaveable {
                mutableStateOf(false)
            }

            Box(Modifier.padding(contentPadding)) {
                LoginWebView(
                    state = state,
                    navigator = navigator,
                    client = webClient,
                    onUp = onUp,
                    onShowAdvancedOptions = { showAdvancedOptions = true },
                )
                if (showAdvancedOptions) {
                    AdvancedOptionsDialog(
                        onDismiss = { showAdvancedOptions = false },
                        loadUrl = { url -> state.content = WebContent.Url(url) },
                        onClickRecheckLoginStatus = onClickRecheckLoginStatus,
                        onClickAlternateLoginPage = onClickAlternateLoginPage,
                        onClickSkipPageRestyling = onClickSkipPageRestyling,
                        onClickCustomIgneousCookie = onClickCustomIgneousCookie,
                    )
                }
            }
        }
    }
}

// The login page with Cancel / Advanced buttons docked under it.
@Composable
private fun LoginWebView(
    state: WebViewState,
    navigator: WebViewNavigator,
    client: AccompanistWebViewClient,
    onUp: () -> Unit,
    onShowAdvancedOptions: () -> Unit,
) {
    Box {
        WebView(
            state = state,
            navigator = navigator,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 48.dp),
            onCreated = { webView ->
                webView.setDefaultSettings()

                // Debug mode (chrome://inspect/#devices)
                if (BuildConfig.DEBUG &&
                    0 != webView.context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE
                ) {
                    WebView.setWebContentsDebuggingEnabled(true)
                }
            },
            client = client,
        )
        Row(
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Button(onClick = onUp, Modifier.weight(HALF)) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
            Button(onClick = onShowAdvancedOptions, Modifier.weight(HALF)) {
                Text(text = stringResource(MR.strings.pref_category_advanced))
            }
        }
    }
}

// Every option closes the dialog; the first three hand the dialog's loadUrl to the caller.
@Composable
private fun AdvancedOptionsDialog(
    onDismiss: () -> Unit,
    loadUrl: (String) -> Unit,
    onClickRecheckLoginStatus: (loadUrl: (String) -> Unit) -> Unit,
    onClickAlternateLoginPage: (loadUrl: (String) -> Unit) -> Unit,
    onClickSkipPageRestyling: (loadUrl: (String) -> Unit) -> Unit,
    onClickCustomIgneousCookie: () -> Unit,
) {
    val options = listOf(
        SYMR.strings.recheck_login_status to { onClickRecheckLoginStatus(loadUrl) },
        SYMR.strings.alternative_login_page to { onClickAlternateLoginPage(loadUrl) },
        SYMR.strings.skip_page_restyling to { onClickSkipPageRestyling(loadUrl) },
        SYMR.strings.custom_igneous_cookie to onClickCustomIgneousCookie,
        MR.strings.action_cancel to {},
    )
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(color = 0xb5000000)),
    ) {
        Dialog(onDismissRequest = onDismiss) {
            Column(Modifier.fillMaxWidth(ADVANCED_OPTIONS_WIDTH)) {
                options.forEach { (label, action) ->
                    Button(
                        onClick = {
                            action()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = stringResource(label))
                    }
                }
            }
        }
    }
}
