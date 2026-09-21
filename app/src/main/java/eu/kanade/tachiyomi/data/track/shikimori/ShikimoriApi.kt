package eu.kanade.tachiyomi.data.track.shikimori

import android.net.Uri
import androidx.core.net.toUri
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.shikimori.dto.SMAddMangaResponse
import eu.kanade.tachiyomi.data.track.shikimori.dto.SMOAuth
import eu.kanade.tachiyomi.data.track.shikimori.dto.SMUser
import eu.kanade.tachiyomi.data.track.shikimori.dto.SMUserListResult
import eu.kanade.tachiyomi.data.track.shikimori.dto.SMUserResult
import eu.kanade.tachiyomi.network.DELETE
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.jsonMime
import eu.kanade.tachiyomi.network.parseAs
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import tachiyomi.core.common.util.lang.withIOContext
import uy.kohesive.injekt.injectLazy
import tachiyomi.domain.track.model.Track as DomainTrack

internal const val VARIABLES = "variables"
private const val CLIENT_ID_KEY = "client_id"
internal const val QUERY = "query"

internal class ShikimoriApi(
    internal val trackId: Long,
    private val client: OkHttpClient,
    interceptor: ShikimoriInterceptor,
) {

    internal val json: Json by injectLazy()

    internal val authClient = client.newBuilder().addInterceptor(interceptor).build()

    suspend fun addLibManga(track: Track, userId: String): Track {
        return withIOContext {
            with(json) {
                val payload = buildJsonObject {
                    putJsonObject("user_rate") {
                        put("user_id", userId)
                        put("target_id", track.remoteId)
                        put("target_type", "Manga")
                        put("chapters", track.lastChapterRead.toInt())
                        put("score", track.score.toInt())
                        put("status", track.toShikimoriStatus())
                    }
                }
                authClient.newCall(
                    POST(
                        "$API_URL/v2/user_rates",
                        body = payload.toString().toRequestBody(jsonMime),
                    ),
                ).awaitSuccess()
                    .parseAs<SMAddMangaResponse>()
                    .let {
                        // save id of the entry for possible future delete request
                        track.libraryId = it.id
                    }
                track
            }
        }
    }

    suspend fun updateLibManga(track: Track, userId: String): Track = addLibManga(track, userId)

    suspend fun deleteLibManga(track: DomainTrack) {
        withIOContext {
            authClient
                .newCall(DELETE("$API_URL/v2/user_rates/${track.libraryId}"))
                .awaitSuccess()
        }
    }

    suspend fun findLibManga(track: Track, isRefresh: Boolean = false): Track? {
        return withIOContext {
            val query = $$"""
                |query($id: String) {
                    |mangas(ids: $id, limit: 1) {
                        |id
                        |url
                        |name
                        |chapters
                        |userRate {
                            |id
                            |chapters
                            |status
                            |score
                        |}
                    |}
                |}
            """.trimMargin()

            val payload = buildJsonObject {
                put(QUERY, query)
                putJsonObject(VARIABLES) {
                    put("id", track.remoteId.toString())
                }
            }
            with(json) {
                val listResult = authClient.newCall(
                    POST(
                        GRAPHQL_API_URL,
                        body = payload.toString().toRequestBody(jsonMime),
                    ),
                )
                    .awaitSuccess()
                    .parseAs<SMUserListResult>()
                    .data
                    .mangas
                    .firstOrNull()

                // Shikimori has no user list query that allows query by ID, so we go via the "mangas" query & include
                // userRate data which will be null if the title is not in the user's list.
                // If it was removed on Shikimori and is still linked in the app, notify user via returning null here
                // which throws an exception at the Shikimori.refresh call
                if (isRefresh && listResult?.userRate == null) {
                    null
                } else {
                    listResult?.toTrack(trackId)
                }
            }
        }
    }

    suspend fun getCurrentUser(): SMUser {
        return with(json) {
            val query = """
                |{
                |currentUser {
                    |id
                    |nickname
                |}
                |}
            """.trimMargin()
            val payload = buildJsonObject {
                put(QUERY, query)
            }
            authClient.newCall(
                POST(
                    GRAPHQL_API_URL,
                    body = payload.toString().toRequestBody(jsonMime),
                ),
            )
                .awaitSuccess()
                .parseAs<SMUserResult>()
                .data
                .currentUser
        }
    }

    suspend fun accessToken(code: String): SMOAuth {
        return withIOContext {
            with(json) {
                client.newCall(accessTokenRequest(code))
                    .awaitSuccess()
                    .parseAs()
            }
        }
    }

    private fun accessTokenRequest(code: String) = POST(
        OAUTH_URL,
        body = FormBody.Builder()
            .add("grant_type", "authorization_code")
            .add(CLIENT_ID_KEY, CLIENT_ID)
            .add("client_secret", CLIENT_SECRET)
            .add("code", code)
            .add("redirect_uri", REDIRECT_URL)
            .build(),
    )

    companion object {
        private const val BASE_URL = "https://shikimori.io"
        private const val API_URL = "$BASE_URL/api"
        internal const val GRAPHQL_API_URL = "$BASE_URL/api/graphql"
        private const val OAUTH_URL = "$BASE_URL/oauth/token"
        private const val LOGIN_URL = "$BASE_URL/oauth/authorize"

        private const val REDIRECT_URL = "mihon://shikimori-auth"

        private const val CLIENT_ID = "PB9dq8DzI405s7wdtwTdirYqHiyVMh--djnP7lBUqSA"
        private const val CLIENT_SECRET = "NajpZcOBKB9sJtgNcejf8OB9jBN1OYYoo-k4h2WWZus"

        fun authUrl(): Uri = LOGIN_URL.toUri().buildUpon()
            .appendQueryParameter(CLIENT_ID_KEY, CLIENT_ID)
            .appendQueryParameter("redirect_uri", REDIRECT_URL)
            .appendQueryParameter("response_type", "code")
            .build()

        fun refreshTokenRequest(token: String) = POST(
            OAUTH_URL,
            body = FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add(CLIENT_ID_KEY, CLIENT_ID)
                .add("client_secret", CLIENT_SECRET)
                .add("refresh_token", token)
                .build(),
        )
    }
}
