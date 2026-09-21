package eu.kanade.tachiyomi.data.track.kitsu

import eu.kanade.tachiyomi.data.track.kitsu.KitsuApi.Companion.GRAPHQL_URL
import eu.kanade.tachiyomi.data.track.kitsu.dto.KitsuMangaMetadata
import eu.kanade.tachiyomi.data.track.kitsu.dto.KitsuMangaMetadataMedia
import eu.kanade.tachiyomi.data.track.model.TrackMangaMetadata
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.jsonMime
import eu.kanade.tachiyomi.network.parseAs
import eu.kanade.tachiyomi.util.lang.htmlDecode
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.Headers.Companion.headersOf
import okhttp3.RequestBody.Companion.toRequestBody
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.track.model.Track as DomainTrack

private const val STAFF_COUNT = 25

private val MANGA_METADATA_QUERY = """
    |query(${'$'}libraryId: ID!, ${'$'}staffCount: Int) {
    |findLibraryEntryById(id: ${'$'}libraryId) {
        |media {
            |id
            |titles {
                |preferred
            |}
            |posterImage {
                |original {
                    |url
                |}
            |}
            |description
            |staff(first: ${'$'}staffCount) {
                |nodes {
                    |role
                    |person {
                        |name
                    |}
                |}
            |}
        |}
    |}
    |}
""".trimMargin()

internal suspend fun KitsuApi.getMangaMetadata(track: DomainTrack): TrackMangaMetadata {
    return withIOContext {
        val payload = buildJsonObject {
            put("query", MANGA_METADATA_QUERY)
            putJsonObject("variables") {
                put("libraryId", track.remoteId)
                put("staffCount", STAFF_COUNT) // based on nothing
            }
        }
        with(json) {
            authClient.newCall(
                POST(
                    GRAPHQL_URL,
                    headers = headersOf("Accept-Language", "en"),
                    body = payload.toString().toRequestBody(jsonMime),
                ),
            )
                .awaitSuccess()
                .parseAs<KitsuMangaMetadata>()
                .data
                .findLibraryEntryById
                .media
                .toTrackMangaMetadata()
        }
    }
}

internal fun KitsuMangaMetadataMedia.toTrackMangaMetadata(): TrackMangaMetadata {
    fun staffNamed(vararg roles: String): String? = staff.nodes
        .filter { it.role in roles }
        .map { it.person.name }
        .joinToString(", ")
        .ifEmpty { null }
    return TrackMangaMetadata(
        remoteId = id.toLong(),
        title = titles.preferred,
        thumbnailUrl = posterImage.original.url,
        description = description.en?.htmlDecode()?.ifEmpty { null },
        authors = staffNamed("Story", "Story & Art"),
        artists = staffNamed("Art", "Story & Art"),
    )
}
