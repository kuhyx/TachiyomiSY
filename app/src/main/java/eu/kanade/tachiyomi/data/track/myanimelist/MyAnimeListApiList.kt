package eu.kanade.tachiyomi.data.track.myanimelist

import androidx.core.net.toUri
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.data.track.myanimelist.MyAnimeListApi.Companion.BASE_API_URL
import eu.kanade.tachiyomi.data.track.myanimelist.MyAnimeListApi.Companion.LIST_PAGINATION_AMOUNT
import eu.kanade.tachiyomi.data.track.myanimelist.MyAnimeListApi.Companion.MANGA_API_URL
import eu.kanade.tachiyomi.data.track.myanimelist.MyAnimeListApi.Companion.SEARCH_FIELDS
import eu.kanade.tachiyomi.data.track.myanimelist.dto.MALListItem
import eu.kanade.tachiyomi.data.track.myanimelist.dto.MALSearchResult
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.parseAs
import okhttp3.Request
import tachiyomi.core.common.util.lang.withIOContext

internal suspend fun MyAnimeListApi.findListItem(track: Track): Track? {
    return withIOContext {
        val uri = MANGA_API_URL.toUri().buildUpon()
            .appendPath(track.remoteId.toString())
            .appendQueryParameter(FIELDS, "num_chapters,my_list_status{start_date,finish_date}")
            .build()
        with(json) {
            authClient.newCall(GET(uri.toString()))
                .awaitSuccess()
                .parseAs<MALListItem>()
                .let { item ->
                    track.totalChapters = item.numChapters
                    item.myListStatus?.let { parseMangaItem(it, track) }
                }
        }
    }
}

internal suspend fun MyAnimeListApi.findListItems(query: String, offset: Int = 0): List<TrackSearch> {
    return withIOContext {
        val myListSearchResult = getListPage(offset)

        val matches = myListSearchResult.data
            .filter { it.node.title.contains(query, ignoreCase = true) }
            .map { parseSearchItem(it.node) }

        // Check next page if there's more
        if (!myListSearchResult.paging.next.isNullOrBlank()) {
            matches + findListItems(query, offset + LIST_PAGINATION_AMOUNT)
        } else {
            matches
        }
    }
}

internal suspend fun MyAnimeListApi.getListPage(offset: Int): MALSearchResult {
    return withIOContext {
        val urlBuilder = "$BASE_API_URL/users/@me/mangalist".toUri().buildUpon()
            .appendQueryParameter(FIELDS, SEARCH_FIELDS)
            .appendQueryParameter("limit", LIST_PAGINATION_AMOUNT.toString())
        if (offset > 0) {
            urlBuilder.appendQueryParameter("offset", offset.toString())
        }

        val request = Request.Builder()
            .url(urlBuilder.build().toString())
            .get()
            .build()
        with(json) {
            authClient.newCall(request)
                .awaitSuccess()
                .parseAs()
        }
    }
}
