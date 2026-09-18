package eu.kanade.tachiyomi.network.interceptor

import android.content.Context
import android.os.Build
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Toast
import eu.kanade.tachiyomi.util.system.DeviceUtil
import eu.kanade.tachiyomi.util.system.WebViewUtil
import eu.kanade.tachiyomi.util.system.setDefaultSettings
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.DelicateCoroutinesApi
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import tachiyomi.core.common.util.lang.launchUI
import tachiyomi.i18n.MR
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private const val WEBVIEW_TIMEOUT_SECONDS = 30L

/** Base for interceptors that replay a failed request inside a WebView. */
public abstract class WebViewInterceptor(
    private val context: Context,
    private val defaultUserAgentProvider: () -> String,
) : Interceptor {

    // When this is called, it initializes the WebView if it wasn't already. We use this to avoid
    // blocking the main thread too much. If used too often we could consider moving it to the
    // Application class.
    private val initWebView by lazy {
        // Crashes on some devices. We skip this in some cases since the only impact is slower
        // WebView init in those rare cases.
        // See https://bugs.chromium.org/p/chromium/issues/detail?id=1279562
        val skipWarmUp = DeviceUtil.isMiui || (Build.VERSION.SDK_INT == Build.VERSION_CODES.S && DeviceUtil.isSamsung)
        if (!skipWarmUp) {
            try {
                WebSettings.getDefaultUserAgent(context)
            } catch (_: Exception) {
                // Avoid some crashes like when Chrome/WebView is being updated.
            }
        }
    }

    /** True when [response] is a challenge this interceptor can solve. */
    public abstract fun shouldIntercept(response: Response): Boolean

    /** Solves the challenge behind [response] and proceeds with [request]. */
    public abstract fun intercept(chain: Interceptor.Chain, request: Request, response: Response): Response

    @OptIn(DelicateCoroutinesApi::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        return when {
            !shouldIntercept(response) -> {
                response
            }
            !WebViewUtil.supportsWebView(context) -> {
                launchUI {
                    context.toast(MR.strings.information_webview_required, Toast.LENGTH_LONG)
                }
                response
            }
            else -> {
                initWebView
                intercept(chain, request, response)
            }
        }
    }

    /** [headers] as a plain map for the WebView. */
    public fun parseHeaders(headers: Headers): Map<String, String> = headers
        // Keeping unsafe header makes webview throw [net::ERR_INVALID_ARGUMENT]
        .filter { (name, value) ->
            isRequestHeaderSafe(name, value)
        }
        .groupBy(keySelector = { (name, _) -> name }) { (_, value) -> value }
        .mapValues { it.value.first() }

    /** Waits at most thirty seconds for the latch. */
    public fun CountDownLatch.awaitFor30Seconds() {
        await(WEBVIEW_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    }

    /** A WebView configured with the request's user agent and headers. */
    public fun createWebView(request: Request): WebView = WebView(context).apply {
        setDefaultSettings()
        // Avoid sending empty User-Agent, Chromium WebView will reset to default if empty
        settings.userAgentString = request.header("User-Agent") ?: defaultUserAgentProvider()
    }
}

// Based on [IsRequestHeaderSafe] in
// https://source.chromium.org/chromium/chromium/src/+/main:services/network/public/cpp/header_util.cc
internal fun isRequestHeaderSafe(rawName: String, rawValue: String): Boolean {
    val name = rawName.lowercase(Locale.ENGLISH)
    val value = rawValue.lowercase(Locale.ENGLISH)
    val unsafeName = name in unsafeHeaderNames || name.startsWith("proxy-")
    val upgrade = name == "connection" && value == "upgrade"
    return !unsafeName && !upgrade
}
private val unsafeHeaderNames = listOf(
    "content-length", "host", "trailer", "te", "upgrade", "cookie2", "keep-alive", "transfer-encoding", "set-cookie",
)
