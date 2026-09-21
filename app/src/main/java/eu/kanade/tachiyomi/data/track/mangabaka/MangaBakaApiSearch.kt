package eu.kanade.tachiyomi.data.track.mangabaka

import androidx.core.net.toUri
import eu.kanade.tachiyomi.data.track.mangabaka.MangaBakaApi.Companion.API_BASE_URL
import eu.kanade.tachiyomi.data.track.mangabaka.MangaBakaApi.Companion.BASE_URL
import eu.kanade.tachiyomi.data.track.mangabaka.dto.MangaBakaItem
import eu.kanade.tachiyomi.data.track.mangabaka.dto.MangaBakaItemResult
import eu.kanade.tachiyomi.data.track.mangabaka.dto.MangaBakaSearchResult
import eu.kanade.tachiyomi.data.track.mangabaka.dto.chooseBestTitle
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.HttpException
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.parseAs
import tachiyomi.core.common.util.lang.withIOContext
import java.math.RoundingMode
import java.net.HttpURLConnection
import java.util.Locale

internal suspend fun MangaBakaApi.search(search: String): List<TrackSearch> {
    return withIOContext {
        val url = "$API_BASE_URL/v1/series/search".toUri().buildUpon()
            .appendQueryParameter("q", search)
            .appendQueryParameter("type_not", "novel")
            .build()
        with(json) {
            client.newCall(GET(url.toString()))
                .awaitSuccess()
                .parseAs<MangaBakaSearchResult>()
                .data
                .map { parseSearchItem(it) }
        }
    }
}

internal fun MangaBakaApi.parseSearchItem(item: MangaBakaItem): TrackSearch {
    return TrackSearch.create(trackId).apply {
        remoteId = item.id
        title = item.chooseBestTitle()
        summary = item.description?.trim().orEmpty()
        score = item.rating?.let { it.toBigDecimal().setScale(2, RoundingMode.HALF_UP).toDouble() } ?: -1.0
        coverUrl = item.cover.x250.x1.orEmpty()
        trackingUrl = "$BASE_URL/${item.id}"
        startDate = item.published.startDate.orEmpty()
        publishingStatus = item.status
        publishingType = item.type.replaceFirstChar { c ->
            if (c.isLowerCase()) c.titlecase(Locale.getDefault()) else c.toString()
        }
        authors = item.authors.orEmpty()
        artists = item.artists.orEmpty()
    }
}

internal suspend fun MangaBakaApi.getMangaDetails(id: Int): TrackSearch? {
    return withIOContext {
        val url = "$API_BASE_URL/v1/series".toUri().buildUpon()
            .appendPath(id.toString())
            .build()
        with(json) {
            try {
                authClient.newCall(GET(url.toString()))
                    .awaitSuccess()
                    .parseAs<MangaBakaItemResult>()
                    .data
                    .let { parseSearchItem(it) }
            } catch (e: HttpException) {
                if (e.code != HttpURLConnection.HTTP_NOT_FOUND) throw e
                null
            }
        }
    }
}
