package eu.kanade.tachiyomi.util.system

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.AndroidRuntimeException
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import kotlinx.coroutines.suspendCancellableCoroutine
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import kotlin.coroutines.resume

/** WebView presence, version and user-agent helpers. */
public object WebViewUtil {
    private const val CHROME_PACKAGE = "com.android.chrome"
    private const val YOUTUBE_FOR_TV_PACKAGE = "com.google.android.youtube.tv"
    private const val SYSTEM_SETTINGS_PACKAGE = "com.android.settings"

    /** Oldest Chromium major version the app supports. */
    public const val MINIMUM_WEBVIEW_VERSION: Int = 118

    /**
     * Uses the WebView's user agent string to create something similar to what Chrome on Android
     * would return.
     *
     * Example of WebView user agent string:
     *   Mozilla/5.0 (Linux; Android 13; Pixel 7 Build/TQ3A.230901.001; wv) AppleWebKit/537.36
     *   (KHTML, like Gecko) Version/4.0 Chrome/116.0.0.0 Mobile Safari/537.36
     *
     * Example of Chrome on Android:
     *   Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.3
     */
    public fun getInferredUserAgent(context: Context): String = WebView(context)
        .getDefaultUserAgentString()
        .replace("; Android .*?\\)".toRegex(), "; Android 10; K)")
        .replace("Version/.* Chrome/".toRegex(), "Chrome/")

    /** The installed WebView's version name. */
    public fun getVersion(context: Context): String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val webView = WebView.getCurrentWebViewPackage() ?: return "how did you get here?"
        val pm = context.packageManager
        val label = webView.applicationInfo!!.loadLabel(pm)
        val version = webView.versionName
        "$label $version"
    } else {
        "Unknown"
    }

    /** True when a usable WebView is installed. */
    public fun supportsWebView(context: Context): Boolean {
        try {
            // May throw android.webkit.WebViewFactory$MissingWebViewPackageException if WebView
            // is not installed
            CookieManager.getInstance()
        } catch (e: AndroidRuntimeException) {
            logcat(LogPriority.ERROR, e)
            return false
        }

        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_WEBVIEW)
    }

    /** Chrome's package name when installed, so sites treat the WebView as Chrome. */
    public fun spoofedPackageName(context: Context): String =
        runCatching { context.packageManager.getPackageInfo(CHROME_PACKAGE, 0) }
            .recoverCatching { context.packageManager.getPackageInfo(SYSTEM_SETTINGS_PACKAGE, 0) }
            .recoverCatching { context.packageManager.getPackageInfo(YOUTUBE_FOR_TV_PACKAGE, 0) }
            .fold(
                onSuccess = { it.packageName },
                onFailure = {
                    context.packageManager.getInstalledPackages(0)
                        .random()
                        .packageName
                },
            )
}

/** True when the WebView is older than [WebViewUtil.MINIMUM_WEBVIEW_VERSION]. */
public fun WebView.isOutdated(): Boolean = getWebViewMajorVersion() < WebViewUtil.MINIMUM_WEBVIEW_VERSION

/** The current page's HTML. */
public suspend fun WebView.getHtml(): String = suspendCancellableCoroutine {
    evaluateJavascript("document.documentElement.outerHTML") { html -> it.resume(html) }
}

/** Enables JavaScript, storage and the app's user agent. */
@SuppressLint("SetJavaScriptEnabled")
public fun WebView.setDefaultSettings() {
    with(settings) {
        javaScriptEnabled = true
        domStorageEnabled = true
        useWideViewPort = true
        loadWithOverviewMode = true
        cacheMode = WebSettings.LOAD_DEFAULT

        // Handle popups properly
        setSupportMultipleWindows(true)

        // Allow zooming
        setSupportZoom(true)
        builtInZoomControls = true
        displayZoomControls = false
    }

    CookieManager.getInstance().acceptThirdPartyCookies(this)
}

private fun WebView.getWebViewMajorVersion(): Int {
    // The single capture group is present whenever the pattern matches at all.
    val uaRegexMatch = """.*Chrome/(\d+)\..*""".toRegex().matchEntire(getDefaultUserAgentString())
    return uaRegexMatch?.groupValues?.get(1)?.toInt() ?: 0
}

// Based on https://stackoverflow.com/a/29218966
private fun WebView.getDefaultUserAgentString(): String {
    val originalUA: String = settings.userAgentString

    // Next call to getUserAgentString() will get us the default
    settings.userAgentString = null
    val defaultUserAgentString = settings.userAgentString

    // Revert to original UA string
    settings.userAgentString = originalUA

    return defaultUserAgentString
}
