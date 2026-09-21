package eu.kanade.tachiyomi.data.track.shikimori

import eu.kanade.tachiyomi.data.track.model.TrackMangaMetadata
import eu.kanade.tachiyomi.data.track.shikimori.dto.SMMetadata
import eu.kanade.tachiyomi.data.track.shikimori.dto.SMMetadataResult
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.jsonMime
import eu.kanade.tachiyomi.network.parseAs
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.RequestBody.Companion.toRequestBody
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.track.model.Track as DomainTrack

private val MANGA_METADATA_QUERY = """
    |query(${'$'}ids: String!) {
        |mangas(ids: ${'$'}ids) {
            |id
            |name
            |description
            |poster {
                |originalUrl
            |}
            |personRoles {
                |person {
                    |name
                |}
                |rolesEn
            |}
        |}
    |}
""".trimMargin()

internal suspend fun ShikimoriApi.getMangaMetadata(track: DomainTrack): TrackMangaMetadata {
    return withIOContext {
        val payload = buildJsonObject {
            put(QUERY, MANGA_METADATA_QUERY)
            putJsonObject(VARIABLES) {
                put("ids", "${track.remoteId}")
            }
        }
        with(json) {
            authClient.newCall(
                POST(
                    "https://shikimori.one/api/graphql",
                    body = payload.toString().toRequestBody(jsonMime),
                ),
            )
                .awaitSuccess()
                .parseAs<SMMetadata>()
                .data
                .mangas
                .firstOrNull()
                ?.toTrackMangaMetadata()
                ?: throw NoSuchElementException("Could not get metadata from Shikimori")
        }
    }
}

internal fun SMMetadataResult.toTrackMangaMetadata(): TrackMangaMetadata {
    fun namedWithRole(vararg roles: String): String? = personRoles
        .filter { role -> roles.any { it in role.roles } }
        .map { it.person.name }
        .joinToString(", ")
        .ifEmpty { null }
    return TrackMangaMetadata(
        remoteId = id.toLong(),
        title = name,
        thumbnailUrl = poster.originalUrl,
        description = description,
        authors = namedWithRole("Story", "Story & Art"),
        artists = namedWithRole("Art", "Story & Art"),
    )
}
