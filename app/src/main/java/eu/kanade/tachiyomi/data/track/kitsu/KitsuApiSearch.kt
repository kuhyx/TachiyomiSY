package eu.kanade.tachiyomi.data.track.kitsu

import eu.kanade.tachiyomi.data.track.kitsu.KitsuApi.Companion.ALGOLIA_APP_ID
import eu.kanade.tachiyomi.data.track.kitsu.KitsuApi.Companion.ALGOLIA_FILTER
import eu.kanade.tachiyomi.data.track.kitsu.KitsuApi.Companion.ALGOLIA_KEY_URL
import eu.kanade.tachiyomi.data.track.kitsu.KitsuApi.Companion.ALGOLIA_URL
import eu.kanade.tachiyomi.data.track.kitsu.dto.KitsuAlgoliaSearchResult
import eu.kanade.tachiyomi.data.track.kitsu.dto.KitsuSearchResult
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.jsonMime
import eu.kanade.tachiyomi.network.parseAs
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.Headers.Companion.headersOf
import okhttp3.RequestBody.Companion.toRequestBody
import tachiyomi.core.common.util.lang.withIOContext
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal suspend fun KitsuApi.search(query: String): List<TrackSearch> {
    return withIOContext {
        with(json) {
            authClient.newCall(GET(ALGOLIA_KEY_URL))
                .awaitSuccess()
                .parseAs<KitsuSearchResult>()
                .let {
                    algoliaSearch(it.media.key, query)
                }
        }
    }
}

internal suspend fun KitsuApi.algoliaSearch(key: String, query: String): List<TrackSearch> {
    return withIOContext {
        val jsonObject = buildJsonObject {
            put("params", "query=${URLEncoder.encode(query, StandardCharsets.UTF_8.name())}$ALGOLIA_FILTER")
        }

        with(json) {
            client.newCall(
                POST(
                    ALGOLIA_URL,
                    headers = headersOf(
                        "X-Algolia-Application-Id",
                        ALGOLIA_APP_ID,
                        "X-Algolia-API-Key",
                        key,
                    ),
                    body = jsonObject.toString().toRequestBody(jsonMime),
                ),
            )
                .awaitSuccess()
                .parseAs<KitsuAlgoliaSearchResult>()
                .hits
                .filter { it.subtype != "novel" }
                .map { it.toTrack() }
        }
    }
}
