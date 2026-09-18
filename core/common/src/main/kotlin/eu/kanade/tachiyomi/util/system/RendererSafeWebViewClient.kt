package eu.kanade.tachiyomi.util.system

import android.webkit.RenderProcessGoneDetail
import android.webkit.WebView
import android.webkit.WebViewClient
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat

/** A [WebViewClient] that survives its renderer dying instead of taking the process down. */
public abstract class RendererSafeWebViewClient : WebViewClient() {
    /**
     * Drops the WebView and reports the event handled, so its owner recreates it on the next use;
     * returning false here (the default) crashes the app.
     */
    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
        logcat(LogPriority.ERROR) { "WebView renderer gone (crash=${detail.didCrash()}); destroying the view" }
        view.destroy()
        return true
    }
}
