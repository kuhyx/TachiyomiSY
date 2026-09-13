package eu.kanade.tachiyomi.util.system

import android.os.Build
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi

/** A [WebViewClient] that funnels the old and new callback overloads into one set of `*Compat` hooks. */
@Suppress("OverridingDeprecatedMember")
public abstract class WebViewClientCompat : WebViewClient() {

    /** [WebViewClient.shouldOverrideUrlLoading] for either overload. */
    public open fun shouldOverrideUrlCompat(view: WebView, url: String): Boolean = false

    /** [WebViewClient.shouldInterceptRequest] for either overload. */
    public open fun shouldInterceptRequestCompat(view: WebView, url: String): WebResourceResponse? = null

    /** [WebViewClient.onReceivedError] for either overload. */
    public open fun onReceivedErrorCompat(
        view: WebView,
        errorCode: Int,
        description: String?,
        failingUrl: String,
        isMainFrame: Boolean,
    ) {
    }

    @RequiresApi(Build.VERSION_CODES.N)
    final override fun shouldOverrideUrlLoading(
        view: WebView,
        request: WebResourceRequest,
    ): Boolean = shouldOverrideUrlCompat(view, request.url.toString())

    @Deprecated("shouldOverrideUrlLoading(WebView, WebResourceRequest)")
    final override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean =
        shouldOverrideUrlCompat(view, url)

    final override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest,
    ): WebResourceResponse? = shouldInterceptRequestCompat(view, request.url.toString())

    @Deprecated("shouldInterceptRequest(WebView, WebResourceRequest)")
    final override fun shouldInterceptRequest(view: WebView, url: String): WebResourceResponse? =
        shouldInterceptRequestCompat(view, url)

    final override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: WebResourceError,
    ) {
        onReceivedErrorCompat(
            view,
            error.errorCode,
            error.description?.toString(),
            request.url.toString(),
            request.isForMainFrame,
        )
    }

    @Deprecated("onReceivedError(WebView, WebResourceRequest, WebResourceError)")
    final override fun onReceivedError(
        view: WebView,
        errorCode: Int,
        description: String?,
        failingUrl: String,
    ) {
        onReceivedErrorCompat(view, errorCode, description, failingUrl, failingUrl == view.url)
    }

    final override fun onReceivedHttpError(
        view: WebView,
        request: WebResourceRequest,
        error: WebResourceResponse,
    ) {
        onReceivedErrorCompat(
            view,
            error.statusCode,
            error.reasonPhrase,
            request.url
                .toString(),
            request.isForMainFrame,
        )
    }
}
