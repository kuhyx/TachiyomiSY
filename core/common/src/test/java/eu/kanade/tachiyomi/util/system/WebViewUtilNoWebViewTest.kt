package eu.kanade.tachiyomi.util.system

import android.content.Context
import android.util.AndroidRuntimeException
import android.webkit.CookieManager
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import logcat.LogPriority
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import tachiyomi.core.common.util.system.RecordingLogcatLogger

/** A [CookieManager] factory that fails the way it does on a device without a WebView package. */
@Implements(CookieManager::class)
internal object ShadowMissingCookieManager {
    @JvmStatic
    @Implementation
    fun getInstance(): CookieManager = throw AndroidRuntimeException("no WebView installed")
}

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowMissingCookieManager::class])
internal class WebViewUtilNoWebViewTest {
    private val context: Context = RuntimeEnvironment.getApplication()

    @Test
    fun missingWebViewIsUnsupported() {
        val logger = RecordingLogcatLogger.start()
        WebViewUtil.supportsWebView(context) shouldBe false
        val entry = logger.entries.single()
        entry.priority shouldBe LogPriority.ERROR
        entry.message shouldContain "no WebView installed"
    }
}
