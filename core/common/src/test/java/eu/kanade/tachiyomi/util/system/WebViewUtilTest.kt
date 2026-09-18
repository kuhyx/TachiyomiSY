package eu.kanade.tachiyomi.util.system

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.webkit.WebSettings
import android.webkit.WebView
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowWebSettings
import org.robolectric.shadows.ShadowWebView
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
internal class WebViewUtilTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val apiLevel = Build.VERSION.SDK_INT

    @After
    fun tearDown() {
        ShadowWebSettings.setDefaultUserAgent(null)
        ShadowWebView.setCurrentWebViewPackage(null)
        ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", apiLevel)
    }

    @Test
    fun inferredAgentLooksLikeChrome() {
        ShadowWebSettings.setDefaultUserAgent(WEBVIEW_UA)
        WebViewUtil.getInferredUserAgent(context) shouldBe
            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/116.0.0.0 Mobile Safari/537.36"
    }

    @Test
    fun versionCombinesLabelAndName() {
        val info = PackageInfo().apply {
            packageName = "com.google.android.webview"
            versionName = "118.0.5993.48"
            applicationInfo = ApplicationInfo().apply { nonLocalizedLabel = "Android System WebView" }
        }
        ShadowWebView.setCurrentWebViewPackage(info)
        WebViewUtil.getVersion(context) shouldBe "Android System WebView 118.0.5993.48"
    }

    @Test
    fun missingPackageHasFallback() {
        WebViewUtil.getVersion(context) shouldBe "how did you get here?"
    }

    @Test
    fun supportFollowsTheSystemFeature() {
        val packageManager = shadowOf(context.packageManager)
        packageManager.setSystemFeature(PackageManager.FEATURE_WEBVIEW, true)
        WebViewUtil.supportsWebView(context) shouldBe true
        packageManager.setSystemFeature(PackageManager.FEATURE_WEBVIEW, false)
        WebViewUtil.supportsWebView(context) shouldBe false
    }

    @Test
    fun spoofPrefersChromeSettingsTv() {
        val packageManager = shadowOf(context.packageManager)
        packageManager.installPackage(PackageInfo().apply { packageName = "com.google.android.youtube.tv" })
        WebViewUtil.spoofedPackageName(context) shouldBe "com.google.android.youtube.tv"
        packageManager.installPackage(PackageInfo().apply { packageName = "com.android.settings" })
        WebViewUtil.spoofedPackageName(context) shouldBe "com.android.settings"
        packageManager.installPackage(PackageInfo().apply { packageName = "com.android.chrome" })
        WebViewUtil.spoofedPackageName(context) shouldBe "com.android.chrome"
    }

    @Test
    fun spoofFallsBackToALaunchableApp() {
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val activity = ActivityInfo().apply {
            packageName = "org.example.launchable"
            name = "org.example.launchable.Main"
        }
        shadowOf(context.packageManager).addOrUpdateActivity(activity)
        shadowOf(context.packageManager).addIntentFilterForActivity(
            ComponentName(activity.packageName, activity.name),
            IntentFilter(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) },
        )
        context.packageManager.queryIntentActivities(launcher, 0).size shouldBe 1
        WebViewUtil.spoofedPackageName(context) shouldBe "org.example.launchable"
    }

    @Test
    fun outdatedComparesChromeMajor() {
        val webView = WebView(context)
        val original = webView.settings.userAgentString
        webView.isOutdated() shouldBe true
        ShadowWebSettings.setDefaultUserAgent(WEBVIEW_UA)
        webView.isOutdated() shouldBe true
        ShadowWebSettings.setDefaultUserAgent(WEBVIEW_UA.replace("116", "118"))
        webView.isOutdated() shouldBe false
        webView.settings.userAgentString shouldBe original
    }

    @Test
    fun htmlResumesOnScriptCallback() {
        val webView = WebView(context)
        runTest {
            val html = async { webView.getHtml() }
            runCurrent()
            val shadow = shadowOf(webView)
            shadow.lastEvaluatedJavascript shouldBe "document.documentElement.outerHTML"
            shadow.lastEvaluatedJavascriptCallback.onReceiveValue("<html/>")
            html.await() shouldBe "<html/>"
        }
    }

    @Test
    fun defaultSettingsEnableFeatures() {
        val webView = WebView(context)
        webView.setDefaultSettings()
        with(webView.settings) {
            javaScriptEnabled shouldBe true
            domStorageEnabled shouldBe true
            useWideViewPort shouldBe true
            loadWithOverviewMode shouldBe true
            cacheMode shouldBe WebSettings.LOAD_DEFAULT
            supportMultipleWindows() shouldBe true
            supportZoom() shouldBe true
            builtInZoomControls shouldBe true
            displayZoomControls shouldBe false
        }
    }

    private companion object {
        const val WEBVIEW_UA =
            "Mozilla/5.0 (Linux; Android 13; Pixel 7 Build/TQ3A.230901.001; wv) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Version/4.0 Chrome/116.0.0.0 Mobile Safari/537.36"
    }
}
