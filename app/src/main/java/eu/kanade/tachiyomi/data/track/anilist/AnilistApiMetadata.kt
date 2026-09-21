package eu.kanade.tachiyomi.data.track.anilist

import eu.kanade.tachiyomi.data.track.anilist.AnilistApi.Companion.API_URL
import eu.kanade.tachiyomi.data.track.anilist.dto.ALMangaMetadata
import eu.kanade.tachiyomi.data.track.anilist.dto.ALMangaMetadataMedia
import eu.kanade.tachiyomi.data.track.model.TrackMangaMetadata
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.jsonMime
import eu.kanade.tachiyomi.network.parseAs
import eu.kanade.tachiyomi.util.lang.htmlDecode
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.RequestBody.Companion.toRequestBody
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.track.model.Track as DomainTrack

private val MANGA_METADATA_QUERY = """
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

internal suspend fun AnilistApi.getMangaMetadata(track: DomainTrack): TrackMangaMetadata {
    return withIOContext {
        val payload = buildJsonObject {
            put(QUERY, MANGA_METADATA_QUERY)
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
                .data
                .media
                .toTrackMangaMetadata()
        }
    }
}

internal fun ALMangaMetadataMedia.toTrackMangaMetadata(): TrackMangaMetadata {
    fun staffNamed(vararg roles: String): String? = staff.edges
        .filter { it.role in roles }
        .map { it.node.name.userPreferred }
        .joinToString(", ")
        .ifEmpty { null }
    return TrackMangaMetadata(
        remoteId = id,
        title = title.userPreferred,
        thumbnailUrl = coverImage.large,
        description = description?.htmlDecode()?.ifEmpty { null },
        authors = staffNamed("Story", "Story & Art"),
        artists = staffNamed("Art", "Story & Art"),
    )
}
