package eu.kanade.tachiyomi.network.interceptor

import android.app.Application
import android.content.pm.PackageManager
import eu.kanade.tachiyomi.network.CannedServer
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.TEST_URL
import eu.kanade.tachiyomi.network.cannedResponse
import eu.kanade.tachiyomi.network.clientOf
import eu.kanade.tachiyomi.util.system.DeviceUtil
import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSystemProperties
import java.util.concurrent.Executor

/**
 * MIUI skips the WebView warm-up. `DeviceUtil.isMiui` is a lazy on a singleton, so it must be read for the
 * first time after the property is overridden: the unique instrumented-packages entry gives this class a
 * sandbox (class loader) of its own instead of the one the other WebView tests share.
 */
@RunWith(RobolectricTestRunner::class)
@Config(shadows = [RecordingWebViewShadow::class], instrumentedPackages = ["sandbox.miui"])
internal class WebViewInterceptorMiuiTest {
    private val app: Application = RuntimeEnvironment.getApplication()
    private val server = CannedServer { cannedResponse(it, code = 403) }

    @Before
    fun setUp() {
        ShadowSystemProperties.override("ro.miui.ui.version.name", "V14")
        shadowOf(app.packageManager).setSystemFeature(PackageManager.FEATURE_WEBVIEW, true)
    }

    @Test
    fun warmUpIsSkippedOnMiui() {
        DeviceUtil.isMiui shouldBe true
        val context = ExecutorContext(app, Executor { it.run() })
        val interceptor = CountingWebViewInterceptor(context, { "ua" }, intercepts = true)
        clientOf(server, interceptor).newCall(GET(TEST_URL)).execute().close()
        interceptor.solved shouldBe 1
    }
}
