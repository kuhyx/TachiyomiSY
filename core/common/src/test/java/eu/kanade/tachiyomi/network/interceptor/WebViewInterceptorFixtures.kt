package eu.kanade.tachiyomi.network.interceptor

import android.content.Context
import android.content.ContextWrapper
import android.webkit.WebView
import android.webkit.WebViewClient
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.RealObject
import org.robolectric.shadows.ShadowWebView
import java.util.concurrent.Executor

/** A context with a test-owned main executor, so work the interceptors post "to the main thread" runs inline. */
internal class ExecutorContext(base: Context, private val executor: Executor) : ContextWrapper(base) {
    override fun getMainExecutor(): Executor = executor
}

/** A [WebViewInterceptor] that intercepts every response when told to and counts the hand-offs. */
internal class CountingWebViewInterceptor(
    context: Context,
    userAgent: () -> String,
    private val intercepts: Boolean,
) : WebViewInterceptor(context, userAgent) {
    var solved: Int = 0

    override fun shouldIntercept(response: Response): Boolean = intercepts

    override fun intercept(chain: Interceptor.Chain, request: Request, response: Response): Response {
        solved += 1
        response.close()
        return chain.proceed(request)
    }
}

/**
 * The stock WebView shadow plus two hooks the Cloudflare tests drive the page events through: [onClient]
 * runs when a client is attached and [onLoad] after every `loadUrl`, both with the real WebView and its
 * client, so a test can play the callbacks the interceptor is waiting on before it blocks on its latch.
 */
@Implements(WebView::class)
internal class RecordingWebViewShadow : ShadowWebView() {
    @field:RealObject
    lateinit var real: WebView

    @Implementation
    public override fun setWebViewClient(client: WebViewClient) {
        webViews += real
        onClient(real, client)
        super.setWebViewClient(client)
    }

    @Implementation
    public override fun loadUrl(url: String, additionalHttpHeaders: Map<String, String>) {
        super.loadUrl(url, additionalHttpHeaders)
        onLoad(real, checkNotNull(webViewClient))
    }

    companion object {
        val webViews: MutableList<WebView> = mutableListOf()
        var onClient: (WebView, WebViewClient) -> Unit = { _, _ -> }
        var onLoad: (WebView, WebViewClient) -> Unit = { _, _ -> }

        fun reset() {
            webViews.clear()
            onClient = { _, _ -> }
            onLoad = { _, _ -> }
        }
    }
}
