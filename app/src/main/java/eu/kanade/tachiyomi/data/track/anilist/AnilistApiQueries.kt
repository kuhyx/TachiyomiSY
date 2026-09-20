package eu.kanade.tachiyomi.data.track.anilist

import eu.kanade.tachiyomi.data.track.anilist.AnilistApi.Companion.API_URL
import eu.kanade.tachiyomi.data.track.anilist.dto.ALCurrentUserResult
import eu.kanade.tachiyomi.data.track.anilist.dto.ALIdSearchResult
import eu.kanade.tachiyomi.data.track.anilist.dto.ALMangaMetadata
import eu.kanade.tachiyomi.data.track.anilist.dto.ALSearchResult
import eu.kanade.tachiyomi.data.track.anilist.dto.ALUserViewerData
import eu.kanade.tachiyomi.data.track.model.TrackMangaMetadata
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.jsonMime
import eu.kanade.tachiyomi.network.parseAs
import eu.kanade.tachiyomi.util.lang.htmlDecode
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.RequestBody.Companion.toRequestBody
import tachiyomi.core.common.util.lang.withIOContext
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import tachiyomi.domain.track.model.Track as DomainTrack

internal suspend fun AnilistApi.search(search: String): List<TrackSearch> {
    return withIOContext {
        val query = $$"""
        |query Search($query: String) {
            |Page (perPage: 50) {
                |media(search: $query, type: MANGA, format_not_in: [NOVEL]) {
                    |id
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
                    |title {
                        |userPreferred
                    |}
                    |coverImage {
                        |large
                    |}
                    |format
                    |countryOfOrigin
                    |status
                    |chapters
                    |description
                    |startDate {
                        |year
                        |month
                        |day
                    |}
                    |averageScore
                |}
            |}
        |}
        |
        """.trimMargin()
        val payload = buildJsonObject {
            put(QUERY, query)
            putJsonObject(VARIABLES) {
                put(QUERY, search)
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
                .parseAs<ALSearchResult>()
                .data
                .page
                .media
                .map { it.toALManga().toTrack() }
        }
    }
}

internal suspend fun AnilistApi.getCurrentUser(): ALUserViewerData {
    return withIOContext {
        val query = """
            |query User {
            |Viewer {
                |id
                |name
                |mediaListOptions {
                    |scoreFormat
                |}
            |}
            |}
            |
        """.trimMargin()
        val payload = buildJsonObject {
            put(QUERY, query)
        }
        with(json) {
            authClient.newCall(
                POST(
                    API_URL,
                    body = payload.toString().toRequestBody(jsonMime),
                ),
            )
                .awaitSuccess()
                .parseAs<ALCurrentUserResult>()
                .data
                .viewer
        }
    }
}

internal suspend fun AnilistApi.getMangaMetadata(track: DomainTrack): TrackMangaMetadata {
    return withIOContext {
        val query = """
            |query (${'$'}mangaId: Int!) {
            |Media (id: ${'$'}mangaId) {
                |id
                |title {
                    |userPreferred
                |}
                |coverImage {
                    |large
                |}
                |description
                |staff {
                    |edges {
                        |role
                        |id
                        |node {
                            |name {
                                |userPreferred
                            |}
                        |}
                    |}
                |}
            |}
            |}
            |
        """.trimMargin()
        val payload = buildJsonObject {
            put(QUERY, query)
            putJsonObject(VARIABLES) {
                put(MANGA_ID, track.remoteId)
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
                .parseAs<ALMangaMetadata>()
                .let {
                    val media = it.data.media
                    TrackMangaMetadata(
                        remoteId = media.id,
                        title = media.title.userPreferred,
                        thumbnailUrl = media.coverImage.large,
                        description = media.description?.htmlDecode()?.ifEmpty { null },
                        authors = media.staff.edges
                            .filter { it.role == "Story" || it.role == "Story & Art" }
                            .map { it.node.name.userPreferred }
                            .joinToString(", ")
                            .ifEmpty { null },
                        artists = media.staff.edges
                            .filter { it.role == "Art" || it.role == "Story & Art" }
                            .map { it.node.name.userPreferred }
                            .joinToString(", ")
                            .ifEmpty { null },
                    )
                }
        }
    }
}

// SY -->
internal suspend fun AnilistApi.searchById(id: String): TrackSearch {
    return withIOContext {
        val query = """
            |query (${'$'}mangaId: Int!) {
            |Media (id: ${'$'}mangaId) {
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
                |averageScore
            |}
            |}
            |
        """.trimMargin()
        val payload = buildJsonObject {
            put(QUERY, query)
            putJsonObject(VARIABLES) {
                put(MANGA_ID, id)
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
                .parseAs<ALIdSearchResult>()
                .data
                .media
                .toALManga()
                .toTrack()
        }
    }
}

internal fun AnilistApi.createDate(dateValue: Long): JsonObject {
    if (dateValue == 0L) {
        return buildJsonObject {
            put("year", JsonNull)
            put("month", JsonNull)
            put("day", JsonNull)
        }
    }

    val dateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(dateValue), ZoneId.systemDefault())
    return buildJsonObject {
        put("year", dateTime.year)
        put("month", dateTime.monthValue)
        put("day", dateTime.dayOfMonth)
    }
}
