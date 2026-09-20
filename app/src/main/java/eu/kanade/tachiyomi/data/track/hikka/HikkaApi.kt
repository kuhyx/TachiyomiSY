package eu.kanade.tachiyomi.data.track.hikka

import android.net.Uri
import androidx.core.net.toUri
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.hikka.dto.HKManga
import eu.kanade.tachiyomi.data.track.hikka.dto.HKMangaPagination
import eu.kanade.tachiyomi.data.track.hikka.dto.HKOAuth
import eu.kanade.tachiyomi.data.track.hikka.dto.HKRead
import eu.kanade.tachiyomi.data.track.hikka.dto.HKUser
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.network.DELETE
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.HttpException
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.PUT
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.jsonMime
import eu.kanade.tachiyomi.network.parseAs
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import tachiyomi.core.common.util.lang.withIOContext
import uy.kohesive.injekt.injectLazy
import java.net.HttpURLConnection
import tachiyomi.domain.track.model.Track as DomainTrack

private const val MAX_SCORE = 10
private const val MILLIS_PER_SECOND = 1000L

// A Hikka tracking URL is https://hikka.io/manga/<slug>; the slug is the fifth `/`-separated piece.
private const val SLUG_URL_SEGMENT = 4

internal class HikkaApi(
    private val trackId: Long,
    private val client: OkHttpClient,
    interceptor: HikkaInterceptor,
) {
    private val json: Json by injectLazy()
    private val authClient = client.newBuilder().addInterceptor(interceptor).build()

    suspend fun getCurrentUser(): HKUser {
        return withIOContext {
            val request = Request.Builder()
                .url("${BASE_API_URL}/user/me")
                .get()
                .build()
            with(json) {
                authClient.newCall(request)
                    .awaitSuccess()
                    .parseAs<HKUser>()
            }
        }
    }

    suspend fun accessToken(reference: String): HKOAuth {
        return withIOContext {
            with(json) {
                client.newCall(authTokenCreate(reference))
                    .awaitSuccess()
                    .parseAs<HKOAuth>()
            }
        }
    }

    suspend fun searchManga(query: String): List<TrackSearch> {
        return withIOContext {
            val url = "$BASE_API_URL/manga".toUri().buildUpon()
                .appendQueryParameter("page", "1")
                .appendQueryParameter("size", "50")
                .build()

            val payload = buildJsonObject {
                put("media_type", buildJsonArray { })
                put("status", buildJsonArray { })
                put("only_translated", false)
                put("magazines", buildJsonArray { })
                put("genres", buildJsonArray { })
                put(
                    "score",
                    buildJsonArray {
                        add(0)
                        add(MAX_SCORE)
                    },
                )
                put("query", query)
                put(
                    "sort",
                    buildJsonArray {
                        add("score:desc")
                        add("scored_by:desc")
                    },
                )
            }

            with(json) {
                authClient.newCall(POST(url.toString(), body = payload.toString().toRequestBody(jsonMime)))
                    .awaitSuccess()
                    .parseAs<HKMangaPagination>()
                    .list
                    .map { it.toTrack(trackId) }
            }
        }
    }

    suspend fun getRead(track: Track): HKRead? {
        return withIOContext {
            val slug = track.trackingUrl.split("/")[SLUG_URL_SEGMENT]
            val url = readMangaUrl(slug).toUri().buildUpon().build()
            with(json) {
                try {
                    authClient.newCall(GET(url.toString()))
                        .awaitSuccess()
                        .parseAs<HKRead>()
                } catch (e: HttpException) {
                    if (e.code == HttpURLConnection.HTTP_NOT_FOUND) {
                        null
                    } else {
                        throw e
                    }
                }
            }
        }
    }

    suspend fun getManga(track: Track): TrackSearch {
        return withIOContext {
            val slug = track.trackingUrl.split("/")[SLUG_URL_SEGMENT]
            val url = "$BASE_API_URL/manga/$slug".toUri().buildUpon()
                .build()

            with(json) {
                authClient.newCall(GET(url.toString()))
                    .awaitSuccess()
                    .parseAs<HKManga>()
                    .toTrack(trackId)
            }
        }
    }

    suspend fun deleteUserManga(track: DomainTrack) {
        return withIOContext {
            val slug = track.remoteUrl.split("/")[SLUG_URL_SEGMENT]

            val url = readMangaUrl(slug).toUri().buildUpon()
                .build()

            authClient.newCall(DELETE(url.toString()))
                .awaitSuccess()
        }
    }

    suspend fun addUserManga(track: Track): Track {
        return withIOContext {
            val slug = track.trackingUrl.split("/")[SLUG_URL_SEGMENT]

            val url = readMangaUrl(slug).toUri().buildUpon()
                .build()

            var rereads = getRead(track)?.rereads ?: 0
            if (track.status == Hikka.REREADING && rereads == 0) {
                rereads = 1
            }

            val payload = buildJsonObject {
                put("note", "")
                put("chapters", track.lastChapterRead.toInt())
                put("volumes", 0)
                put("rereads", rereads)
                put("score", track.score.toInt())
                put("status", track.toApiStatus())
                put("start_date", track.startedReadingDate.toApiSeconds())
                put("end_date", track.finishedReadingDate.toApiSeconds())
            }

            with(json) {
                authClient.newCall(PUT(url.toString(), body = payload.toString().toRequestBody(jsonMime)))
                    .awaitSuccess()
                    .parseAs<HKRead>()
                    .toTrack(trackId)
            }
        }
    }

    suspend fun updateUserManga(track: Track): Track = addUserManga(track)

    companion object {
        const val BASE_API_URL = "https://api.hikka.io"

        const val BASE_URL = "https://hikka.io"
        private const val SCOPE = "readlist,read:user-details"
        private const val CLIENT_REFERENCE = "598ef1f5-b9d2-4e66-8b65-06949d5e14fc"
        private const val CLIENT_SECRET = "OKwzrNOZxq40psFgfcCUYddnvaeZWDnd34rt7fdcB5GmHoBBQuNTWX" +
            "61sZs8KECEWVXtMUDtq8QC4t9WX4DwWWYLXEVlgnlUXGT1fWCb-18c" +
            "Zd2m8Co-8HN6JQcjoP-B"

        private fun readMangaUrl(slug: String) = "$BASE_API_URL/read/manga/$slug"

        fun authUrl(): Uri = "$BASE_URL/oauth".toUri().buildUpon()
            .appendQueryParameter("reference", CLIENT_REFERENCE)
            .appendQueryParameter("scope", SCOPE)
            .build()

        fun refreshTokenRequest(accessToken: String): Request {
            val headers = Headers.Builder()
                .add("auth", accessToken)
                .build()

            return GET("$BASE_API_URL/user/me", headers = headers) // Any request with auth
        }

        fun authTokenCreate(reference: String): Request {
            val payload = buildJsonObject {
                put("request_reference", reference)
                put("client_secret", CLIENT_SECRET)
            }
            return POST("$BASE_API_URL/auth/token", body = payload.toString().toRequestBody(jsonMime))
        }

        fun authTokenInfo(accessToken: String): Request {
            val headers = Headers.Builder()
                .add("auth", accessToken)
                .build()

            return GET("$BASE_API_URL/auth/token/info", headers = headers)
        }
    }
}

// Hikka takes epoch seconds and null for "unset"; the app stores 0 for unset.
internal fun Long.toApiSeconds(): Long? = if (this > 0L) this / MILLIS_PER_SECOND else null
