package eu.kanade.tachiyomi.network.interceptor

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebView
import android.widget.Toast
import androidx.core.content.ContextCompat
import eu.kanade.tachiyomi.network.AndroidCookieJar
import eu.kanade.tachiyomi.util.system.WebViewClientCompat
import eu.kanade.tachiyomi.util.system.isOutdated
import eu.kanade.tachiyomi.util.system.toast
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import java.io.IOException
import java.util.concurrent.CountDownLatch

/** Solves Cloudflare challenges in a WebView and retries the request with the clearance cookie. */
public class CloudflareInterceptor(
    private val context: Context,
    private val cookieManager: AndroidCookieJar,
    defaultUserAgentProvider: () -> String,
) : WebViewInterceptor(context, defaultUserAgentProvider) {

    private val executor = ContextCompat.getMainExecutor(context)

    // True when Cloudflare's anti-bot page answered instead of the site.
    override fun shouldIntercept(response: Response): Boolean =
        response.code in ERROR_CODES && response.header("Server") in SERVER_CHECK

    override fun intercept(
        chain: Interceptor.Chain,
        request: Request,
        response: Response,
    ): Response {
        try {
            response.close()
            cookieManager.remove(request.url, COOKIE_NAMES, 0)
            val oldCookie = cookieManager.get(request.url)
                .firstOrNull { it.name == CLEARANCE_COOKIE }
            resolveWithWebView(request, oldCookie)

            return chain.proceed(request)
        } catch (e: CloudflareBypassException) {
            // Anything else reaches UncaughtExceptionInterceptor, which turns it into an IOException.
            throw IOException(context.stringResource(MR.strings.information_cloudflare_bypass_failure), e)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun resolveWithWebView(originalRequest: Request, oldCookie: Cookie?) {
        // We need to lock this thread until the WebView finds the challenge solution url, because
        // OkHttp doesn't support asynchronous interceptors.
        val latch = CountDownLatch(1)
        var webview: WebView? = null
        val origRequestUrl = originalRequest.url.toString()
        val watcher = ChallengeWatcher(origRequestUrl, oldCookie, latch)
        val headers = parseHeaders(originalRequest.headers)

        executor.execute {
            val created = createWebView(originalRequest)
            created.webViewClient = watcher
            webview = created
            created.loadUrl(origRequestUrl, headers)
        }

        latch.awaitFor30Seconds()

        var isWebViewOutdated = false
        executor.execute {
            if (!watcher.cloudflareBypassed) {
                isWebViewOutdated = webview?.isOutdated() == true
            }

            webview?.run {
                stopLoading()
                destroy()
            }
        }

        // Throw exception if we failed to bypass Cloudflare
        if (!watcher.cloudflareBypassed) {
            // Prompt user to update WebView if it seems too outdated
            if (isWebViewOutdated) {
                context.toast(MR.strings.information_webview_outdated, Toast.LENGTH_LONG)
            }

            throw CloudflareBypassException()
        }
    }

    // Watches the challenge page and releases the latch once the clearance cookie appears, or as
    // soon as it is clear that no challenge was served.
    private inner class ChallengeWatcher(
        private val origRequestUrl: String,
        private val oldCookie: Cookie?,
        private val latch: CountDownLatch,
    ) : WebViewClientCompat() {
        private var challengeFound = false
        var cloudflareBypassed = false
            private set

        override fun onPageFinished(view: WebView, url: String) {
            if (isCloudFlareBypassed()) {
                cloudflareBypassed = true
                latch.countDown()
            }

            if (url == origRequestUrl && !challengeFound) {
                // The first request didn't return the challenge, abort.
                latch.countDown()
            }
        }

        private fun isCloudFlareBypassed(): Boolean = cookieManager.get(origRequestUrl.toHttpUrl())
            .firstOrNull { it.name == CLEARANCE_COOKIE }
            .let { it != null && it != oldCookie }

        override fun onReceivedErrorCompat(
            view: WebView,
            errorCode: Int,
            description: String?,
            failingUrl: String,
            isMainFrame: Boolean,
        ) {
            if (isMainFrame) {
                if (errorCode in ERROR_CODES) {
                    // Found the Cloudflare challenge page.
                    challengeFound = true
                } else {
                    // Unlock thread, the challenge wasn't found.
                    latch.countDown()
                }
            }
        }
    }
}

private const val HTTP_FORBIDDEN = 403
private const val HTTP_SERVICE_UNAVAILABLE = 503
private val ERROR_CODES = listOf(HTTP_FORBIDDEN, HTTP_SERVICE_UNAVAILABLE)
private val SERVER_CHECK = arrayOf("cloudflare-nginx", "cloudflare")
private const val CLEARANCE_COOKIE = "cf_clearance"
private val COOKIE_NAMES = listOf(CLEARANCE_COOKIE)

private class CloudflareBypassException : Exception()
