package eu.kanade.tachiyomi.network.interceptor

import android.app.Application
import android.content.pm.PackageManager
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import eu.kanade.tachiyomi.network.AndroidCookieJar
import eu.kanade.tachiyomi.network.CannedServer
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.TEST_URL
import eu.kanade.tachiyomi.network.cannedResponse
import eu.kanade.tachiyomi.network.clientOf
import eu.kanade.tachiyomi.util.system.WebViewClientCompat
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import okhttp3.Headers.Companion.headersOf
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast
import org.robolectric.shadows.ShadowWebSettings
import java.io.IOException
import java.util.concurrent.Executor

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [RecordingWebViewShadow::class])
internal class CloudflareInterceptorTest {
    private val app: Application = RuntimeEnvironment.getApplication()
    private val jar = AndroidCookieJar()
    private val request = GET(TEST_URL, headersOf("Accept", "text/html", "User-Agent", "ua/2"))
    private var turns = 0
    private val server = CannedServer { req ->
        turns += 1
        if (turns == 1) challenge(req) else cannedResponse(req)
    }
    private val interceptor = CloudflareInterceptor(ExecutorContext(app, Executor { it.run() }), jar) { "ua/1" }
    private val client = clientOf(server, interceptor)

    @Before
    fun setUp() {
        RecordingWebViewShadow.reset()
        shadowOf(app.packageManager).setSystemFeature(PackageManager.FEATURE_WEBVIEW, true)
    }

    @Test
    fun challengeIsCloudflareErrorPage() {
        val challenges = listOf(403 to "cloudflare", 503 to "cloudflare-nginx")
        challenges.forEach { (code, served) ->
            withClue("$code $served") { interceptor.shouldIntercept(response(code, served)) shouldBe true }
        }
        interceptor.shouldIntercept(response(403, "nginx")) shouldBe false
        interceptor.shouldIntercept(response(200, "cloudflare")) shouldBe false
        interceptor.shouldIntercept(cannedResponse(request, code = 503)) shouldBe false
    }

    @Test
    fun solvedChallengeRetriesRequest() {
        RecordingWebViewShadow.onLoad = { view, pageClient ->
            CookieManager.getInstance().setCookie(TEST_URL, "$CLEARANCE=fresh")
            pageClient.onPageFinished(view, TEST_URL)
        }
        client.newCall(request).execute().use { it.code shouldBe 200 }
        server.requests.size shouldBe 2
        val webView = RecordingWebViewShadow.webViews.single()
        shadowOf(webView).lastLoadedUrl shouldBe TEST_URL
        shadowOf(webView).lastAdditionalHttpHeaders shouldBe mapOf(
            "Accept" to "text/html",
            "User-Agent" to "ua/2",
            "Cache-Control" to "max-age=600",
        )
        webView.settings.userAgentString shouldBe "ua/2"
        shadowOf(webView).wasDestroyCalled() shouldBe true
        jar.get(TEST_URL.toHttpUrl()).single().value shouldBe "fresh"
    }

    @Test
    fun unchangedCookieAbortsAndWarns() {
        // The stale clearance cookie is expired before the WebView runs; seeing that same (now empty) cookie
        // again after the page load is not a bypass, and the outdated default WebView earns the update toast.
        CookieManager.getInstance().setCookie(TEST_URL, "$CLEARANCE=stale")
        RecordingWebViewShadow.onLoad = { view, pageClient -> pageClient.onPageFinished(view, TEST_URL) }
        shouldThrow<IOException> { client.newCall(request).execute() }.message shouldBe BYPASS_FAILURE
        ShadowToast.getTextOfLatestToast() shouldBe "Please update the WebView app for better compatibility"
        server.requests.size shouldBe 1
    }

    @Test
    fun challengePageThenCookieRetries() {
        RecordingWebViewShadow.onLoad = { view, pageClient ->
            pageClient.pageError(view, code = 503, isMainFrame = true)
            pageClient.pageError(view, code = 404, isMainFrame = false)
            pageClient.onPageFinished(view, TEST_URL)
            CookieManager.getInstance().setCookie(TEST_URL, "$CLEARANCE=fresh")
            pageClient.onPageFinished(view, "$TEST_URL?__cf_chl_tk=1")
        }
        client.newCall(request).execute().use { it.code shouldBe 200 }
        ShadowToast.shownToastCount() shouldBe 0
        server.requests.size shouldBe 2
    }

    @Test
    fun otherErrorAbortsWithoutToast() {
        ShadowWebSettings.setDefaultUserAgent("Mozilla/5.0 (Linux; Android 14) Chrome/130.0.0.0 Mobile Safari/537.36")
        RecordingWebViewShadow.onLoad = { view, pageClient ->
            pageClient.pageError(view, code = 404, isMainFrame = true)
        }
        shouldThrow<IOException> { client.newCall(request).execute() }.message shouldBe BYPASS_FAILURE
        ShadowToast.shownToastCount() shouldBe 0
        server.requests.size shouldBe 1
    }

    @Test
    fun failedWebViewSetupAborts() {
        // The client hook releases the interceptor's latch and then fails, so no WebView ever gets attached.
        RecordingWebViewShadow.onClient = { view, pageClient ->
            pageClient.pageError(view, code = 404, isMainFrame = true)
            error("WebView unavailable")
        }
        val swallowing = Executor { task ->
            try {
                task.run()
            } catch (_: IllegalStateException) {
                // The hook failed on purpose; the interceptor has to cope with a missing WebView.
            }
        }
        val failing = clientOf(server, CloudflareInterceptor(ExecutorContext(app, swallowing), jar) { "ua/1" })
        shouldThrow<IOException> { failing.newCall(request).execute() }.message shouldBe BYPASS_FAILURE
        ShadowToast.shownToastCount() shouldBe 0
        server.requests.size shouldBe 1
    }

    private fun challenge(req: Request): Response =
        cannedResponse(req, code = 403, headers = headersOf("Server", "cloudflare"))

    private fun response(code: Int, server: String): Response =
        cannedResponse(request, code = code, headers = headersOf("Server", server))

    // Plays a main-frame or sub-frame page error into the interceptor's own WebViewClient.
    private fun WebViewClient.pageError(view: WebView, code: Int, isMainFrame: Boolean) {
        val watcher = shouldBeInstanceOf<WebViewClientCompat>()
        watcher.onReceivedErrorCompat(
            view = view,
            errorCode = code,
            description = null,
            failingUrl = TEST_URL,
            isMainFrame = isMainFrame,
        )
    }

    private companion object {
        const val CLEARANCE = "cf_clearance"
        const val BYPASS_FAILURE = "Failed to bypass Cloudflare"
    }
}
