package exh.ui.login

import android.webkit.CookieManager
import java.net.HttpCookie
import java.util.Locale

internal data class ExhLogin(val memberId: String, val passHash: String, val igneous: String)

internal fun cookiesFor(url: String): List<HttpCookie>? =
    CookieManager.getInstance().getCookie(url)?.let { cookie ->
        cookie.split("; ").flatMap { HttpCookie.parse(it) }
    }

// Logged in at the forums once both the member id and the pass hash cookies carry a value.
internal fun List<HttpCookie>.hasForumLogin(): Boolean = count {
    (
        it.name.equals(EhLoginActivity.MEMBER_ID_COOKIE, ignoreCase = true) ||
            it.name.equals(EhLoginActivity.PASS_HASH_COOKIE, ignoreCase = true)
        ) &&
        it.value.isNotBlank()
} >= 2

// The three cookies ExHentai needs, or null while any is still missing; a custom igneous wins over the site's.
internal fun List<HttpCookie>.toExhLogin(customIgneous: String?): ExhLogin? {
    val byName = associate { it.name.lowercase(Locale.getDefault()) to it.value }
    val memberId = byName[EhLoginActivity.MEMBER_ID_COOKIE]
    val passHash = byName[EhLoginActivity.PASS_HASH_COOKIE]
    val igneous = customIgneous ?: byName[EhLoginActivity.IGNEOUS_COOKIE]
    return if (memberId == null || passHash == null || igneous == null) {
        null
    } else {
        ExhLogin(memberId, passHash, igneous)
    }
}
