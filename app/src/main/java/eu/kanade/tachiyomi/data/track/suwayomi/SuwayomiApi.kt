package eu.kanade.tachiyomi.data.track.suwayomi

import android.content.SharedPreferences
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.jsonMime
import eu.kanade.tachiyomi.network.parseAs
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.sourceIdOf
import eu.kanade.tachiyomi.source.sourcePreferences
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.addAll
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.injectLazy

private const val VARIABLES = "variables"
private const val MANGA_ID = "mangaId"
private const val QUERY = "query"

private const val CHAPTER_EPSILON = 0.001

private val UNREAD_CHAPTERS_QUERY = $$"""
|query GetMangaUnreadChapters($mangaId: Int!) {
|  chapters(condition: {mangaId: $mangaId, isRead: false}) {
|    nodes {
|      id
|      chapterNumber
|    }
|  }
|}
""".trimMargin()

private val MARK_READ_MUTATION = $$"""
|mutation MarkChaptersRead($chapters: [Int!]!) {
|  updateChapters(input: {ids: $chapters, patch: {isRead: true}}) {
|    __typename
|  }
|}
""".trimMargin()

private val MARK_READ_AND_DELETE_MUTATION = $$"""
|mutation MarkChaptersRead($chapters: [Int!]!) {
|  updateChapters(input: {ids: $chapters, patch: {isRead: true}}) {
|    __typename
|  }
|  deleteDownloadedChapters(input: {ids: $chapters}) {
|    __typename
|  }
|}
""".trimMargin()

private val TRACK_PROGRESS_MUTATION = $$"""
|mutation TrackManga($mangaId: Int!) {
|  trackProgress(input: {mangaId: $mangaId}) {
|    __typename
|  }
|}
""".trimMargin()

private fun graphQlPayload(query: String, variables: JsonObjectBuilder.() -> Unit): JsonObject = buildJsonObject {
    put(QUERY, query)
    putJsonObject(VARIABLES, variables)
}

internal class SuwayomiApi(private val trackId: Long) {

    private val json: Json by injectLazy()

    private val sourceManager: SourceManager by injectLazy()
    private val source: HttpSource by lazy { sourceManager.get(sourceId) as HttpSource }
    private val configurableSource: ConfigurableSource by lazy { sourceManager.get(sourceId) as ConfigurableSource }
    private val client: OkHttpClient by lazy { source.client }
    private val baseUrl: String by lazy { source.baseUrl.trimEnd('/') }
    private val apiUrl: String by lazy { "$baseUrl/api/graphql" }

    private val sourceId by lazy { sourceIdOf(name = "Tachidesk", lang = "en", versionId = 1) }

    fun sourcePreferences(): SharedPreferences = configurableSource.sourcePreferences()

    suspend fun getTrackSearch(mangaId: Long): TrackSearch = withIOContext {
        val query = $$"""
        |query GetManga($mangaId: Int!) {
        |    manga(id: $mangaId) {
        |        ...MangaFragment
        |    }
        |}
        |
        |$$MangaFragment
        """.trimMargin()
        val payload = buildJsonObject {
            put(QUERY, query)
            putJsonObject(VARIABLES) {
                put(MANGA_ID, mangaId)
            }
        }
        val manga = with(json) {
            client.newCall(
                POST(
                    apiUrl,
                    body = payload.toString().toRequestBody(jsonMime),
                ),
            )
                .awaitSuccess()
                .parseAs<GetMangaResult>()
                .data
                .entry
        }

        TrackSearch.create(trackId).apply {
            remoteId = mangaId
            title = manga.title
            coverUrl = "$baseUrl/${manga.thumbnailUrl}"
            summary = manga.description.orEmpty()
            trackingUrl = "$baseUrl/manga/$mangaId"
            totalChapters = manga.chapters.totalCount.toLong()
            publishingStatus = manga.status.name
            lastChapterRead = manga.latestReadChapter?.chapterNumber ?: 0.0
            status = when (manga.unreadCount) {
                manga.chapters.totalCount -> Suwayomi.UNREAD
                0 -> Suwayomi.COMPLETED
                else -> Suwayomi.READING
            }
        }
    }

    suspend fun updateProgress(track: Track, deleteDownloadsOnServer: Boolean = false): Track {
        val mangaId = track.remoteId
        // Follow-up: Include a filter on the chapter number here (https://github.com/kuhyx/TachiyomiSY/issues/19)
        // Below, we only consider older chapters; since v2.1.1985 filtering works properly in the query
        val chaptersToMark = with(json) {
            post(graphQlPayload(UNREAD_CHAPTERS_QUERY) { put(MANGA_ID, mangaId) })
                .parseAs<GetMangaUnreadChaptersResult>()
                .data
                .entry
                .nodes
                .mapNotNull { n -> n.id.takeIf { n.chapterNumber <= track.lastChapterRead + CHAPTER_EPSILON } }
        }
        val markQuery = if (deleteDownloadsOnServer) MARK_READ_AND_DELETE_MUTATION else MARK_READ_MUTATION
        post(graphQlPayload(markQuery) { putJsonArray("chapters") { addAll(chaptersToMark) } })
        post(graphQlPayload(TRACK_PROGRESS_MUTATION) { put(MANGA_ID, mangaId) })
        return getTrackSearch(track.remoteId)
    }

    private suspend fun post(payload: JsonObject): Response =
        client.newCall(POST(apiUrl, body = payload.toString().toRequestBody(jsonMime))).awaitSuccess()

    companion object {
        private val MangaFragment = """
            |fragment MangaFragment on MangaType {
            |    artist
            |    author
            |    description
            |    id
            |    status
            |    thumbnailUrl
            |    title
            |    url
            |    genre
            |    inLibraryAt
            |    chapters {
            |        totalCount
            |    }
            |    latestUploadedChapter {
            |        uploadDate
            |    }
            |    latestFetchedChapter {
            |        fetchedAt
            |    }
            |    latestReadChapter {
            |        lastReadAt
            |        chapterNumber
            |    }
            |    unreadCount
            |    downloadCount
            |}
        """.trimMargin()
    }
}
