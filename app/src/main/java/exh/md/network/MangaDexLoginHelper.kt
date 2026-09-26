package exh.md.network

import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.tachiyomi.data.track.mdlist.MdList
import eu.kanade.tachiyomi.data.track.myanimelist.dto.MALOAuth
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.parseAs
import exh.md.utils.MdApi
import exh.md.utils.MdConstants
import exh.md.utils.MdUtil
import exh.md.utils.getPkceChallengeCode
import exh.md.utils.loadOAuth
import logcat.LogPriority
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.OkHttpClient
import tachiyomi.core.common.util.system.logcat

internal class MangaDexLoginHelper(
    private val client: OkHttpClient,
    private val preferences: TrackPreferences,
    private val mdList: MdList,
    private val mangaDexAuthInterceptor: MangaDexAuthInterceptor,
) {

    /**
     *  Login given the generated authorization code.
     */
    suspend fun login(authorizationCode: String): Boolean {
        val loginFormBody = FormBody.Builder()
            .add("client_id", MdConstants.Login.clientId)
            .add("grant_type", MdConstants.Login.authorizationCode)
            .add("code", authorizationCode)
            .add("code_verifier", MdUtil.getPkceChallengeCode())
            .add("redirect_uri", MdConstants.Login.redirectUri)
            .build()

        val error = kotlin.runCatching {
            val data = with(MdUtil.jsonParser) {
                client.newCall(
                    POST(MdApi.baseAuthUrl + MdApi.token, body = loginFormBody),
                ).awaitSuccess().parseAs<MALOAuth>()
            }
            mangaDexAuthInterceptor.setAuth(data)
        }.exceptionOrNull()

        if (error != null) {
            logcat(LogPriority.ERROR, error) { "Error logging in" }
            mdList.logout()
        }
        return error == null
    }

    suspend fun logout(): Boolean {
        // Both tokens are non-null once stored: only the whole record can be missing.
        val oauth = MdUtil.loadOAuth(preferences, mdList)
        if (oauth == null || oauth.refreshToken.isEmpty() || oauth.accessToken.isEmpty()) {
            mdList.logout()
            return true
        }
        val sessionToken = oauth.accessToken
        val refreshToken = oauth.refreshToken

        val formBody = FormBody.Builder()
            .add("client_id", MdConstants.Login.clientId)
            .add("refresh_token", refreshToken)
            .add("redirect_uri", MdConstants.Login.redirectUri)
            .build()

        val error = kotlin.runCatching {
            client.newCall(
                POST(
                    url = MdApi.baseAuthUrl + MdApi.logout,
                    headers = Headers.Builder().add("Authorization", "Bearer $sessionToken")
                        .build(),
                    body = formBody,
                ),
            ).awaitSuccess()
            mdList.logout()
        }.exceptionOrNull()

        if (error == null) {
            mangaDexAuthInterceptor.setAuth(null)
        } else {
            logcat(LogPriority.ERROR, error) { "Error logging out" }
        }
        return error == null
    }
}
