package eu.kanade.tachiyomi.data.track.mangaupdates

import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.mangaupdates.MangaUpdates.Companion.READING_LIST
import eu.kanade.tachiyomi.data.track.mangaupdates.MangaUpdates.Companion.WISH_LIST
import eu.kanade.tachiyomi.data.track.mangaupdates.dto.MUListItem
import eu.kanade.tachiyomi.data.track.mangaupdates.dto.MURating
import eu.kanade.tachiyomi.network.DELETE
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.PUT
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.parseAs
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import uy.kohesive.injekt.injectLazy
import java.net.HttpURLConnection
import tachiyomi.domain.track.model.Track as DomainTrack

internal class MangaUpdatesApi(
    interceptor: MangaUpdatesInterceptor,
    internal val client: OkHttpClient,
) {
    internal val json: Json by injectLazy()

    internal val authClient by lazy {
        client.newBuilder()
            .addInterceptor(interceptor)
            .build()
    }

    suspend fun getSeriesListItem(track: Track): Pair<MUListItem, MURating?> {
        val listItem = with(json) {
            authClient.newCall(GET("$BASE_URL/v1/lists/series/${track.remoteId}"))
                .awaitSuccess()
                .parseAs<MUListItem>()
        }

        val rating = getSeriesRating(track)

        return listItem to rating
    }

    suspend fun addSeriesToList(track: Track, hasReadChapters: Boolean) {
        val status = if (hasReadChapters) READING_LIST else WISH_LIST
        val body = buildJsonArray {
            addJsonObject {
                putJsonObject("series") {
                    put("id", track.remoteId)
                }
                put("list_id", status)
            }
        }
        authClient.newCall(
            POST(
                url = "$BASE_URL/v1/lists/series",
                body = body.toString().toRequestBody(CONTENT_TYPE),
            ),
        )
            .awaitSuccess()
            .let {
                if (it.code == HttpURLConnection.HTTP_OK) {
                    track.status = status
                    track.lastChapterRead = 1.0
                }
            }
    }

    suspend fun updateSeriesListItem(track: Track) {
        val body = buildJsonArray {
            addJsonObject {
                putJsonObject("series") {
                    put("id", track.remoteId)
                }
                put("list_id", track.status)
                putJsonObject("status") {
                    put("chapter", track.lastChapterRead.toInt())
                }
            }
        }
        authClient.newCall(
            POST(
                url = "$BASE_URL/v1/lists/series/update",
                body = body.toString().toRequestBody(CONTENT_TYPE),
            ),
        )
            .awaitSuccess()

        updateSeriesRating(track)
    }

    suspend fun deleteSeriesFromList(track: DomainTrack) {
        val body = buildJsonArray {
            add(track.remoteId)
        }
        authClient.newCall(
            POST(
                url = "$BASE_URL/v1/lists/series/delete",
                body = body.toString().toRequestBody(CONTENT_TYPE),
            ),
        )
            .awaitSuccess()
    }

    private suspend fun getSeriesRating(track: Track): MURating? {
        return try {
            with(json) {
                authClient.newCall(GET(ratingUrl(track)))
                    .awaitSuccess()
                    .parseAs<MURating>()
            }
        } catch (_: Exception) {
            // Any failure ends here and the fallback below applies.
            null
        }
    }

    private suspend fun updateSeriesRating(track: Track) {
        if (track.score < 0.0) return
        if (track.score != 0.0) {
            val body = buildJsonObject {
                put("rating", track.score)
            }
            authClient.newCall(
                PUT(
                    url = ratingUrl(track),
                    body = body.toString().toRequestBody(CONTENT_TYPE),
                ),
            )
                .awaitSuccess()
        } else {
            authClient.newCall(
                DELETE(url = ratingUrl(track)),
            )
                .awaitSuccess()
        }
    }

    // SY <--

    companion object {
        internal const val BASE_URL = "https://api.mangaupdates.com"

        internal val CONTENT_TYPE = "application/json".toMediaType()

        internal fun ratingUrl(track: Track) = "$BASE_URL/v1/series/${track.remoteId}/rating"
    }
}
