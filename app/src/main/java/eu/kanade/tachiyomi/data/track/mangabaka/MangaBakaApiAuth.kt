package eu.kanade.tachiyomi.data.track.mangabaka

import eu.kanade.tachiyomi.data.track.mangabaka.MangaBakaApi.Companion.API_BASE_URL
import eu.kanade.tachiyomi.data.track.mangabaka.MangaBakaApi.Companion.CLIENT_ID
import eu.kanade.tachiyomi.data.track.mangabaka.MangaBakaApi.Companion.OAUTH_URL
import eu.kanade.tachiyomi.data.track.mangabaka.MangaBakaApi.Companion.REDIRECT_URI
import eu.kanade.tachiyomi.data.track.mangabaka.MangaBakaApi.Companion.SCOPES
import eu.kanade.tachiyomi.data.track.mangabaka.MangaBakaApi.Companion.codeVerifier
import eu.kanade.tachiyomi.data.track.mangabaka.dto.MangaBakaOAuth
import eu.kanade.tachiyomi.data.track.mangabaka.dto.MangaBakaUserProfile
import eu.kanade.tachiyomi.data.track.mangabaka.dto.MangaBakaUserProfileResponse
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.parseAs
import okhttp3.FormBody
import tachiyomi.core.common.util.lang.withIOContext

private const val REDIRECT_URI_KEY = "redirect_uri"
private const val CLIENT_ID_KEY = "client_id"

internal suspend fun MangaBakaApi.getCurrentUser(): MangaBakaUserProfile {
    return withIOContext {
        with(json) {
            authClient.newCall(GET("$API_BASE_URL/v1/my/profile"))
                .awaitSuccess()
                .parseAs<MangaBakaUserProfileResponse>()
                .data
        }
    }
}

internal suspend fun MangaBakaApi.getAccessToken(code: String): MangaBakaOAuth {
    return withIOContext {
        val formBody = FormBody.Builder()
            .add(CLIENT_ID_KEY, CLIENT_ID)
            .add("code", code)
            .add("code_verifier", codeVerifier)
            .add("code_challenge_method", "S256")
            .add("grant_type", "authorization_code")
            .add(REDIRECT_URI_KEY, REDIRECT_URI)
            .add("scope", SCOPES)
            .build()

        with(json) {
            client.newCall(POST("${OAUTH_URL}/token", body = formBody))
                .awaitSuccess()
                .parseAs()
        }
    }
}
