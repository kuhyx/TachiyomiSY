package exh.ui.login

import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.core.net.toUri
import exh.log.xLogD
import exh.source.ExhPreferences
import uy.kohesive.injekt.injectLazy

/**
 * What [EhLoginActivity] does whenever its web view finishes a page: restyle the forum login, move
 * on to ExHentai once logged in there, and store the ExHentai cookies. [onLoggedIn] ends the flow.
 */
internal class EhLoginPageHandler(private val onLoggedIn: () -> Unit) {
    private val exhPreferences: ExhPreferences by injectLazy()

    fun onPageFinished(view: WebView, url: String, customIgneous: String?) {
        xLogD(url)
        val parsedUrl = url.toUri()
        when {
            parsedUrl.host.equals("forums.e-hentai.org", ignoreCase = true) -> {
                onForumsPageFinished(view, url, parsedUrl)
            }
            // At ExHentai, check that everything worked out...
            parsedUrl.host.equals("exhentai.org", ignoreCase = true) && applyExHentaiCookies(url, customIgneous) -> {
                exhPreferences.enableExhentai.set(true)
                onLoggedIn()
            }
        }
    }

    private fun onForumsPageFinished(view: WebView, url: String, parsedUrl: Uri) {
        view.evaluateJavascript(
            """
                (function() {
                    let html = document.documentElement.innerHTML;
                    return html.includes("/cdn-cgi/");
                })();
            """.trimIndent(),
        ) { result ->
            if (result == "true") {
                xLogD("Cloudflare block detected — skipping logic")
            } else {
                // Hide distracting content
                if (!parsedUrl.queryParameterNames.contains(EhLoginActivity.PARAM_SKIP_INJECT)) {
                    view.evaluateJavascript(EhLoginActivity.HIDE_JS, null)
                }
                // Check login result
                if (parsedUrl.getQueryParameter("code")?.toInt() != 0 && cookiesFor(url)?.hasForumLogin() == true) {
                    view.loadUrl("https://exhentai.org/")
                }
            }
        }
    }

    // Parse cookies at ExHentai.
    private fun applyExHentaiCookies(url: String, customIgneous: String?): Boolean {
        val parsed = cookiesFor(url) ?: return false
        if (customIgneous != null) {
            CookieManager.getInstance().setCookie(url, "${EhLoginActivity.IGNEOUS_COOKIE}=$customIgneous")
        }
        val login = parsed.toExhLogin(customIgneous)
        // Missing a cookie
        if (login == null) return false

        // Update prefs
        exhPreferences.memberIdVal.set(login.memberId)
        exhPreferences.passHashVal.set(login.passHash)
        exhPreferences.igneousVal.set(login.igneous)
        return true
    }
}
