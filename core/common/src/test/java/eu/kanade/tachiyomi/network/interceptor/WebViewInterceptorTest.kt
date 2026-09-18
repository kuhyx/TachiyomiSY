package eu.kanade.tachiyomi.network.interceptor

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.os.Looper
import eu.kanade.tachiyomi.network.CannedServer
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.TEST_URL
import eu.kanade.tachiyomi.network.cannedResponse
import eu.kanade.tachiyomi.network.clientOf
import eu.kanade.tachiyomi.util.system.DeviceUtil
import io.kotest.matchers.shouldBe
import okhttp3.Headers.Companion.headersOf
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast
import org.robolectric.util.ReflectionHelpers
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [RecordingWebViewShadow::class])
internal class WebViewInterceptorTest {
    private val app: Application = RuntimeEnvironment.getApplication()
    private val server = CannedServer { cannedResponse(it, code = 403) }

    @Before
    fun setUp() {
        RecordingWebViewShadow.reset()
        shadowOf(app.packageManager).setSystemFeature(PackageManager.FEATURE_WEBVIEW, true)
    }

    @Test
    fun otherResponsesPassThrough() {
        val interceptor = interceptor(intercepts = false)
        clientOf(server, interceptor).newCall(GET(TEST_URL)).execute().use { it.code shouldBe 403 }
        interceptor.solved shouldBe 0
        server.requests.size shouldBe 1
    }

    @Test
    fun missingWebViewToastsInstead() {
        shadowOf(app.packageManager).setSystemFeature(PackageManager.FEATURE_WEBVIEW, false)
        val interceptor = interceptor(intercepts = true)
        clientOf(server, interceptor).newCall(GET(TEST_URL)).execute().use { it.code shouldBe 403 }
        interceptor.solved shouldBe 0
        shadowOf(Looper.getMainLooper()).idle()
        ShadowToast.getTextOfLatestToast() shouldBe "WebView is required for the app to function"
    }

    @Test
    fun availableWebViewHandsOver() {
        val interceptor = interceptor(intercepts = true)
        clientOf(server, interceptor).newCall(GET(TEST_URL)).execute().close()
        interceptor.solved shouldBe 1
        server.requests.size shouldBe 2
    }

    @Test
    fun warmUpRunsOnNonSamsungS() {
        // Android 12 only skips the warm-up on Samsung devices; Robolectric reports another manufacturer.
        val sdk = Build.VERSION.SDK_INT
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", Build.VERSION_CODES.S)
        try {
            val interceptor = interceptor(intercepts = true)
            clientOf(server, interceptor).newCall(GET(TEST_URL)).execute().close()
            interceptor.solved shouldBe 1
            DeviceUtil.isSamsung shouldBe false
        } finally {
            ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", sdk)
        }
    }

    @Test
    fun createWebViewPrefersRequestUa() {
        val webView = interceptor(intercepts = true).createWebView(GET(TEST_URL, headersOf("User-Agent", "ua/req")))
        webView.settings.userAgentString shouldBe "ua/req"
        webView.settings.javaScriptEnabled shouldBe true
    }

    @Test
    fun createWebViewFallsBackToUa() {
        interceptor(intercepts = true).createWebView(GET(TEST_URL)).settings.userAgentString shouldBe "ua/default"
    }

    @Test
    fun parseHeadersDropsUnsafeOnes() {
        val headers = headersOf(
            "Accept", "text/html",
            "accept", "*/*",
            "Host", "example.com",
            "Proxy-Authorization", "x",
            "Connection", "Upgrade",
            "Keep-Alive", "timeout=5",
            "X-Custom", "1",
        )
        val parsed = interceptor(intercepts = true).parseHeaders(headers)
        parsed shouldBe mapOf("Accept" to "text/html", "accept" to "*/*", "X-Custom" to "1")
    }

    @Test
    fun parseHeadersKeepsConnection() {
        val headers = headersOf("Connection", "keep-alive", "Accept", "text/html", "Accept", "text/plain")
        interceptor(intercepts = true).parseHeaders(headers) shouldBe
            mapOf("Connection" to "keep-alive", "Accept" to "text/html")
    }

    @Test
    fun awaitReturnsOnceCountedDown() {
        with(interceptor(intercepts = true)) {
            CountDownLatch(0).awaitFor30Seconds()
        }
    }

    private fun interceptor(intercepts: Boolean): CountingWebViewInterceptor =
        CountingWebViewInterceptor(ExecutorContext(app, Executor { it.run() }), { "ua/default" }, intercepts)
}
