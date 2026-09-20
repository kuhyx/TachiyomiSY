package eu.kanade.tachiyomi.source.online.all

import androidx.core.net.toUri
import exh.ui.login.EhLoginActivity

internal fun EHentai.spPref() = if (exh) {
    exhPreferences.exhSettingsProfile
} else {
    exhPreferences.ehSettingsProfile
}

internal fun EHentai.rawCookies(sp: Int): Map<String, String> {
    val cookies: MutableMap<String, String> = mutableMapOf()
    if (exhPreferences.enableExhentai.get()) {
        cookies[EhLoginActivity.MEMBER_ID_COOKIE] = exhPreferences.memberIdVal.get()
        cookies[EhLoginActivity.PASS_HASH_COOKIE] = exhPreferences.passHashVal.get()
        cookies[EhLoginActivity.IGNEOUS_COOKIE] = exhPreferences.igneousVal.get()
        cookies["sp"] = sp.toString()

        val sessionKey = exhPreferences.exhSettingsKey.get()
        if (sessionKey.isNotBlank()) {
            cookies["sk"] = sessionKey
        }

        val sessionCookie = exhPreferences.exhSessionCookie.get()
        if (sessionCookie.isNotBlank()) {
            cookies["s"] = sessionCookie
        }

        val hathPerksCookie = exhPreferences.exhHathPerksCookies.get()
        if (hathPerksCookie.isNotBlank()) {
            cookies["hath_perks"] = hathPerksCookie
        }
    }

    // Session-less extended display mode (for users without ExHentai)
    cookies["sl"] = "dm_2"

    // Ignore all content warnings
    cookies["nw"] = "1"

    return cookies
}

internal fun EHentai.cookiesHeader(sp: Int = spPref().get()) = EHentai.buildCookies(rawCookies(sp))

internal fun EHentai.addParam(url: String, param: String, value: String) = url.toUri()
    .buildUpon()
    .appendQueryParameter(param, value)
    .toString()
