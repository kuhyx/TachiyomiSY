package eu.kanade.tachiyomi.network.interceptor

import android.content.Context
import android.content.pm.PackageManager
import android.webkit.CookieManager
import eu.kanade.tachiyomi.network.CannedServer
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.TEST_URL
import eu.kanade.tachiyomi.network.cannedResponse
import eu.kanade.tachiyomi.network.clientOf
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

/**
 * Without Robolectric every android.jar method throws, which is exactly the failure the WebView warm-up
 * swallows: `WebSettings.getDefaultUserAgent` blowing up must not stop the interceptor from handing over.
 */
internal class WebViewInterceptorStubTest {
    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun warmUpFailureIsIgnored() {
        mockkStatic(CookieManager::class)
        every { CookieManager.getInstance() } returns mockk()
        val packageManager: PackageManager = mockk {
            every { hasSystemFeature(PackageManager.FEATURE_WEBVIEW) } returns true
        }
        val context: Context = mockk {
            every { getPackageManager() } returns packageManager
        }
        val interceptor = CountingWebViewInterceptor(context, { "ua" }, intercepts = true)
        val server = CannedServer { cannedResponse(it, code = 403) }
        clientOf(server, interceptor).newCall(GET(TEST_URL)).execute().close()
        interceptor.solved shouldBe 1
        server.requests.size shouldBe 2
    }
}
