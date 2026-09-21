package eu.kanade.tachiyomi.data.track.shikimori

import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.data.track.shikimori.ShikimoriApi.Companion.GRAPHQL_API_URL
import eu.kanade.tachiyomi.data.track.shikimori.dto.SMSearchResult
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.jsonMime
import eu.kanade.tachiyomi.network.parseAs
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.RequestBody.Companion.toRequestBody
import tachiyomi.core.common.util.lang.withIOContext

internal suspend fun ShikimoriApi.search(search: String): List<TrackSearch> {
    return withIOContext {
        val query = $$"""
        |query($query: String) {
            |mangas(search: $query, limit: 20, kind:"!light_novel,!novel") {
                |id
                |name
                |chapters
                |kind
                |poster {
                    |mainUrl
                |}
                |score
                |url
                |status
                |airedOn {
                    |date
                |}
                |description
                |personRoles {
                    |person {
                        |name
                    |}
                    |rolesEn
                |}
            |}
        |}
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
                    GRAPHQL_API_URL,
                    body = payload.toString().toRequestBody(jsonMime),
                ),
            )
                .awaitSuccess()
                .parseAs<SMSearchResult>()
                .data
                .mangas
                .map { it.toTrack(trackId) }
        }
    }
}
