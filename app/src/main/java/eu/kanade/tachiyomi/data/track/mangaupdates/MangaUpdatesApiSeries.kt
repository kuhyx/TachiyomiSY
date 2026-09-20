package eu.kanade.tachiyomi.data.track.mangaupdates

import eu.kanade.tachiyomi.data.track.mangaupdates.MangaUpdatesApi.Companion.BASE_URL
import eu.kanade.tachiyomi.data.track.mangaupdates.MangaUpdatesApi.Companion.CONTENT_TYPE
import eu.kanade.tachiyomi.data.track.mangaupdates.dto.MUContext
import eu.kanade.tachiyomi.data.track.mangaupdates.dto.MUCurrentUser
import eu.kanade.tachiyomi.data.track.mangaupdates.dto.MULoginResponse
import eu.kanade.tachiyomi.data.track.mangaupdates.dto.MURecord
import eu.kanade.tachiyomi.data.track.mangaupdates.dto.MUSearchResult
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.PUT
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.parseAs
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import tachiyomi.domain.track.model.Track as DomainTrack

internal suspend fun MangaUpdatesApi.search(query: String): List<MURecord> {
    val body = buildJsonObject {
        put("search", query)
        put(
            "filter_types",
            buildJsonArray {
                add("drama cd")
                add("novel")
            },
        )
    }

    return with(json) {
        client.newCall(
            POST(
                url = "$BASE_URL/v1/series/search",
                body = body.toString().toRequestBody(CONTENT_TYPE),
            ),
        )
            .awaitSuccess()
            .parseAs<MUSearchResult>()
            .results
            .map { it.record }
    }
}

internal suspend fun MangaUpdatesApi.authenticate(username: String, password: String): MUContext {
    val body = buildJsonObject {
        put("username", username)
        put("password", password)
    }
    return with(json) {
        client.newCall(
            PUT(
                url = "$BASE_URL/v1/account/login",
                body = body.toString().toRequestBody(CONTENT_TYPE),
            ),
        )
            .awaitSuccess()
            .parseAs<MULoginResponse>()
            .context
    }
}

internal suspend fun MangaUpdatesApi.getCurrentUser(): MUCurrentUser {
    return with(json) {
        authClient.newCall(GET("$BASE_URL/v1/account/profile"))
            .awaitSuccess()
            .parseAs<MUCurrentUser>()
    }
}

internal suspend fun MangaUpdatesApi.getSeries(track: DomainTrack): MURecord =
    getSeries(track.remoteId)

// SY -->
internal suspend fun MangaUpdatesApi.getSeries(remoteId: Long): MURecord {
    return with(json) {
        client.newCall(GET("$BASE_URL/v1/series/$remoteId"))
            .awaitSuccess()
            .parseAs<MURecord>()
    }
}

internal suspend fun MangaUpdatesApi.convertToNewId(legacyId: Int): String? =
    client.newBuilder()
        .followRedirects(false)
        .build()
        .newCall(GET("https://www.mangaupdates.com/series.html?id=$legacyId"))
        .await()
        .takeIf(Response::isRedirect)
        ?.header("Location")
        ?.let {
            // Extract the new id from the redirected URL
            Regex("""/series/(\w+)(/([\w-]+)?)?/?${'$'}""")
                .find(it)
                ?.groups
                ?.get(1)
                ?.value
        }
