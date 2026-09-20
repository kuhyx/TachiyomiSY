package exh.md.utils

import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.tachiyomi.data.track.mdlist.MdList
import eu.kanade.tachiyomi.data.track.myanimelist.dto.MALOAuth
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.util.PkceUtil
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import uy.kohesive.injekt.api.get

internal fun MdUtil.saveOAuth(preferences: TrackPreferences, mdList: MdList, oAuth: MALOAuth?) {
    if (oAuth == null) {
        preferences.trackToken(mdList).delete()
    } else {
        preferences.trackToken(mdList).set(jsonParser.encodeToString(oAuth))
    }
}

internal fun MdUtil.loadOAuth(preferences: TrackPreferences, mdList: MdList): MALOAuth? {
    return try {
        jsonParser.decodeFromString<MALOAuth>(preferences.trackToken(mdList).get())
    } catch (_: Exception) {
        null
    }
}

internal fun MdUtil.refreshTokenRequest(oauth: MALOAuth): Request {
    val formBody = FormBody.Builder()
        .add("client_id", MdConstants.Login.clientId)
        .add("grant_type", MdConstants.Login.refreshToken)
        .add("refresh_token", oauth.refreshToken)
        .add("code_verifier", getPkceChallengeCode())
        .add("redirect_uri", MdConstants.Login.redirectUri)
        .build()

    // Add the Authorization header manually as this particular
    // request is called by the interceptor itself so it doesn't reach
    // the part where the token is added automatically.
    val headers = Headers.Builder()
        .add("Authorization", "Bearer ${oauth.accessToken}")
        .build()

    return POST(MdApi.baseAuthUrl + MdApi.token, body = formBody, headers = headers)
}

internal fun MdUtil.getPkceChallengeCode(): String =
    codeVerifier ?: PkceUtil.generateCodeVerifier().also { codeVerifier = it }

internal inline fun <reified T> MdUtil.encodeToBody(body: T): RequestBody {
    return jsonParser.encodeToString(body)
        .toRequestBody("application/json".toMediaType())
}
