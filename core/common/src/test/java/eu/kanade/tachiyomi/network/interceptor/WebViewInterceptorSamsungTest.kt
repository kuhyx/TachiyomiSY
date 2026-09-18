package eu.kanade.tachiyomi.network.interceptor

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import eu.kanade.tachiyomi.network.CannedServer
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.TEST_URL
import eu.kanade.tachiyomi.network.cannedResponse
import eu.kanade.tachiyomi.network.clientOf
import eu.kanade.tachiyomi.util.system.DeviceUtil
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowBuild
import org.robolectric.util.ReflectionHelpers
import java.util.concurrent.Executor

/**
 * Samsung devices on Android 12 skip the WebView warm-up. `DeviceUtil.isSamsung` is a lazy on a singleton,
 * so the manufacturer must be set before it is first read: the unique instrumented-packages entry gives
 * this class a sandbox (class loader) of its own.
 */
@RunWith(RobolectricTestRunner::class)
@Config(shadows = [RecordingWebViewShadow::class], instrumentedPackages = ["sandbox.samsung"])
internal class WebViewInterceptorSamsungTest {
    private val app: Application = RuntimeEnvironment.getApplication()
    private val server = CannedServer { cannedResponse(it, code = 403) }
    private val sdk = Build.VERSION.SDK_INT

    @Before
    fun setUp() {
        ShadowBuild.setManufacturer("samsung")
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", Build.VERSION_CODES.S)
        shadowOf(app.packageManager).setSystemFeature(PackageManager.FEATURE_WEBVIEW, true)
    }

    @After
    fun tearDown() {
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", sdk)
    }

    @Test
    fun warmUpIsSkippedOnSamsungS() {
        DeviceUtil.isSamsung shouldBe true
        val context = ExecutorContext(app, Executor { it.run() })
        val interceptor = CountingWebViewInterceptor(context, { "ua" }, intercepts = true)
        clientOf(server, interceptor).newCall(GET(TEST_URL)).execute().close()
        interceptor.solved shouldBe 1
        DeviceUtil.isMiui shouldBe false
    }
}
