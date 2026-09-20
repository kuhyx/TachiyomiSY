package eu.kanade.tachiyomi.data.track.anilist

import android.net.Uri
import androidx.core.net.toUri
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.anilist.dto.ALAddMangaResult
import eu.kanade.tachiyomi.data.track.anilist.dto.ALOAuth
import eu.kanade.tachiyomi.data.track.anilist.dto.ALUserListMangaQueryResult
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.network.jsonMime
import eu.kanade.tachiyomi.network.parseAs
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import tachiyomi.core.common.util.lang.withIOContext
import uy.kohesive.injekt.injectLazy
import kotlin.time.Duration.Companion.minutes
import tachiyomi.domain.track.model.Track as DomainTrack

internal const val VARIABLES = "variables"
internal const val MANGA_ID = "mangaId"
internal const val QUERY = "query"

// AniList tokens live a year.
private const val YEAR_MILLIS = 365L * 24L * 60L * 60L * 1000L

private val FIND_LIB_MANGA_QUERY = $$"""
|query ($id: Int!, $manga_id: Int!) {
    |Page {
        |mediaList(userId: $id, type: MANGA, mediaId: $manga_id) {
            |id
            |status
            |scoreRaw: score(format: POINT_100)
            |progress
            |private
            |startedAt {
                |year
                |month
                |day
            |}
            |completedAt {
                |year
                |month
                |day
            |}
            |media {
                |id
                |title {
                    |userPreferred
                |}
                |coverImage {
                    |large
                |}
                |format
                |status
                |chapters
                |description
                |startDate {
                    |year
                    |month
                    |day
                |}
                |staff {
                    |edges {
                        |role
                        |id
                        |node {
                            |name {
                                |full
                                |userPreferred
                                |native
                            |}
                        |}
                    |}
                |}
            |}
        |}
    |}
|}
|
""".trimMargin()

internal class AnilistApi(val client: OkHttpClient, interceptor: AnilistInterceptor) {

    internal val json: Json by injectLazy()

    internal val authClient = client.newBuilder()
        .addInterceptor(interceptor)
        .rateLimit(permits = 85, period = 1.minutes)
        .build()

    suspend fun addLibManga(track: Track): Track {
        return withIOContext {
            val query = $$"""
            |mutation AddManga($mangaId: Int, $progress: Int, $status: MediaListStatus, $private: Boolean) {
                |SaveMediaListEntry (mediaId: $mangaId, progress: $progress, status: $status, private: $private) {
                |   id
                |   status
                |}
            |}
            |
            """.trimMargin()
            val payload = buildJsonObject {
                put(QUERY, query)
                putJsonObject(VARIABLES) {
                    put(MANGA_ID, track.remoteId)
                    put("progress", track.lastChapterRead.toInt())
                    put("status", track.toApiStatus())
                    put("private", track.private)
                }
            }
            with(json) {
                authClient.newCall(
                    POST(
                        API_URL,
                        body = payload.toString().toRequestBody(jsonMime),
                    ),
                )
                    .awaitSuccess()
                    .parseAs<ALAddMangaResult>()
                    .let {
                        track.libraryId = it.data.entry.id
                        track
                    }
            }
        }
    }

    suspend fun updateLibManga(track: Track): Track {
        return withIOContext {
            val query = $$"""
            |mutation UpdateManga(
                |$listId: Int, $progress: Int, $status: MediaListStatus, $private: Boolean,
                |$score: Int, $startedAt: FuzzyDateInput, $completedAt: FuzzyDateInput
            |) {
                |SaveMediaListEntry(
                    |id: $listId, progress: $progress, status: $status, private: $private,
                    |scoreRaw: $score, startedAt: $startedAt, completedAt: $completedAt
                |) {
                    |id
                    |status
                    |progress
                |}
            |}
            |
            """.trimMargin()
            val payload = buildJsonObject {
                put(QUERY, query)
                putJsonObject(VARIABLES) {
                    put("listId", track.libraryId)
                    put("progress", track.lastChapterRead.toInt())
                    put("status", track.toApiStatus())
                    put("score", track.score.toInt())
                    put("startedAt", createDate(track.startedReadingDate))
                    put("completedAt", createDate(track.finishedReadingDate))
                    put("private", track.private)
                }
            }
            authClient.newCall(POST(API_URL, body = payload.toString().toRequestBody(jsonMime)))
                .awaitSuccess()
            track
        }
    }

    suspend fun deleteLibManga(track: DomainTrack) {
        withIOContext {
            val query = $$"""
            |mutation DeleteManga($listId: Int) {
                |DeleteMediaListEntry(id: $listId) {
                    |deleted
                |}
            |}
            |
            """.trimMargin()
            val payload = buildJsonObject {
                put(QUERY, query)
                putJsonObject(VARIABLES) {
                    put("listId", track.libraryId)
                }
            }
            authClient.newCall(POST(API_URL, body = payload.toString().toRequestBody(jsonMime)))
                .awaitSuccess()
        }
    }

    suspend fun findLibManga(track: Track, userid: Int): Track? {
        return withIOContext {
            val payload = buildJsonObject {
                put(QUERY, FIND_LIB_MANGA_QUERY)
                putJsonObject(VARIABLES) {
                    put("id", userid)
                    put("manga_id", track.remoteId)
                }
            }
            with(json) {
                authClient.newCall(
                    POST(
                        API_URL,
                        body = payload.toString().toRequestBody(jsonMime),
                    ),
                )
                    .awaitSuccess()
                    .parseAs<ALUserListMangaQueryResult>()
                    .data
                    .page
                    .mediaList
                    .map { it.toALUserManga() }
                    .firstOrNull()
                    ?.toTrack()
            }
        }
    }

    suspend fun getLibManga(track: Track, userId: Int): Track =
        findLibManga(track, userId) ?: throw NoSuchElementException("Could not find manga")

    fun createOAuth(token: String): ALOAuth =
        ALOAuth(token, "Bearer", System.currentTimeMillis() + YEAR_MILLIS, YEAR_MILLIS)

    // SY <--

    companion object {
        private const val CLIENT_ID = "16329"
        internal const val API_URL = "https://graphql.anilist.co/"
        private const val BASE_URL = "https://anilist.co/api/v2/"
        private const val BASE_MANGA_URL = "https://anilist.co/manga/"

        fun mangaUrl(mediaId: Long): String = BASE_MANGA_URL + mediaId

        fun authUrl(): Uri = "${BASE_URL}oauth/authorize".toUri().buildUpon()
            .appendQueryParameter("client_id", CLIENT_ID)
            .appendQueryParameter("response_type", "token")
            .build()
    }
}
