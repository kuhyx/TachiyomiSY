package exh.md.network

import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.tachiyomi.data.track.mdlist.MdList
import eu.kanade.tachiyomi.data.track.myanimelist.dto.MALOAuth
import eu.kanade.tachiyomi.data.track.myanimelist.dto.isExpired
import eu.kanade.tachiyomi.network.parseAs
import exh.md.utils.MdUtil
import exh.md.utils.loadOAuth
import exh.md.utils.refreshTokenRequest
import exh.md.utils.saveOAuth
import exh.util.nullIfBlank
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import tachiyomi.core.common.util.system.logcat
import java.io.IOException
import java.net.HttpURLConnection

internal class MangaDexAuthInterceptor(
    private val trackPreferences: TrackPreferences,
    private val mdList: MdList,
) : Interceptor {

    var token = trackPreferences.trackToken(mdList).get().nullIfBlank()

    private var oauth: MALOAuth? = null

    override fun intercept(chain: Interceptor.Chain): Response {
        if (token.isNullOrEmpty()) return chain.proceed(chain.request())

        val originalRequest = chain.request()
        val response = chain.proceed(originalRequest.withBearer(currentAuth(chain).accessToken))
        val tokenIsExpired = response.headers["www-authenticate"]
            ?.contains("The access token expired")
            ?: false

        // Retry the request once with a new token in case it was not already refreshed
        // by the is expired check before.
        return if (response.code == HttpURLConnection.HTTP_UNAUTHORIZED && tokenIsExpired) {
            retryWithFreshToken(chain, originalRequest, response)
        } else {
            response
        }
    }

    // The stored OAuth, refreshed when expired; having none at all cannot be recovered from here.
    private fun currentAuth(chain: Interceptor.Chain): MALOAuth {
        if (oauth == null) {
            oauth = MdUtil.loadOAuth(trackPreferences, mdList)
        }
        // Refresh access token if expired
        if (oauth?.isExpired() == true) {
            setAuth(refreshToken(chain))
        }
        return oauth ?: throw IOException("No authentication token")
    }

    // The original response stands when no fresh token could be fetched.
    private fun retryWithFreshToken(chain: Interceptor.Chain, originalRequest: Request, response: Response): Response {
        val newToken = refreshToken(chain)
        setAuth(newToken)
        if (newToken == null) return response
        response.close()
        return chain.proceed(originalRequest.withBearer(newToken.accessToken))
    }

    /**
     * Called when the user authenticates with MangaDex for the first time. Sets the refresh token
     * and the oauth object.
     */
    fun setAuth(oauth: MALOAuth?) {
        token = oauth?.accessToken
        this.oauth = oauth
        MdUtil.saveOAuth(trackPreferences, mdList, oauth)
    }

    private fun refreshToken(chain: Interceptor.Chain): MALOAuth? {
        val newOauth = runCatching {
            val oauthResponse = chain.proceed(MdUtil.refreshTokenRequest(oauth!!))

            if (oauthResponse.isSuccessful) {
                with(MdUtil.jsonParser) { oauthResponse.parseAs<MALOAuth>() }
            } else {
                oauthResponse.close()
                null
            }
        }

        logcat(throwable = newOauth.exceptionOrNull()) { "Fetched new mangadex oauth" }

        return newOauth.getOrNull()
    }
}

// Add the authorization header to the original request
internal fun Request.withBearer(accessToken: String): Request = newBuilder()
    .addHeader("Authorization", "Bearer $accessToken")
    .build()
