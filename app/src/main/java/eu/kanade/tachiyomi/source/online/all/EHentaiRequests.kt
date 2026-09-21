package eu.kanade.tachiyomi.source.online.all

import androidx.core.net.toUri
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.MetadataMangasPage
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.asJsoup
import exh.util.UriFilter
import exh.util.nullIfBlank
import kotlinx.serialization.json.add
import okhttp3.CacheControl
import okhttp3.Headers
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

// Parse a list of galleries.
internal fun EHentai.genericMangaParse(
    response: Response,
) = galleryListParser.parse(response.asJsoup()).let { (parsedManga, nextPage) ->
    MetadataMangasPage(
        parsedManga.map { it.manga },
        nextPage != null,
        parsedManga.map { it.metadata },
        nextPage,
    )
}

internal fun EHentai.exGet(
    url: String,
    next: Int? = null,
    prev: Int? = null,
    additionalHeaders: Headers? = null,
    cacheControl: CacheControl? = null,
): Request {
    return GET(
        when {
            next != null && next > 1 -> addParam(url, "next", next.toString())
            prev != null && prev > 0 -> addParam(url, "prev", prev.toString())
            else -> url
        },
        if (additionalHeaders != null) {
            val headers = headers.newBuilder()
            additionalHeaders.toMultimap().forEach { (t, u) ->
                u.forEach {
                    headers.add(t, it)
                }
            }
            headers.build()
        } else {
            headers
        },
    ).let {
        if (cacheControl == null) {
            it
        } else {
            it.newBuilder().cacheControl(cacheControl).build()
        }
    }
}

// The gallery page, as HttpSource's deprecated `mangaDetailsRequest` default builds it.
internal fun EHentai.galleryRequest(manga: SManga): Request = GET(baseUrl + manga.url, headers)

internal fun EHentai.checkValid(page: MangasPage): MangasPage =
    if (exh && page.mangas.isEmpty() && exhPreferences.igneousVal.get().equals("mystery", true)) {
        throw IOException(
            "Invalid igneous cookie, try re-logging or finding a correct one to input in the login menu",
        )
    } else {
        page
    }

internal fun EHentai.searchRequest(page: Int, query: String, filters: FilterList): Request {
    val toplist = ToplistOption.entries[filters.firstNotNullOfOrNull { (it as? ToplistOptions)?.state } ?: 0]
    if (toplist != ToplistOption.NONE) return exGet(url = toplistUrl(toplist, page))

    val uri = baseUrl.toUri().buildUpon()
    val isReverseFilterEnabled = filters.any { it is ReverseFilter && it.state }
    val jumpSeekValue = filters.firstNotNullOfOrNull { (it as? JumpSeekFilter)?.state?.nullIfBlank() }

    uri.appendQueryParameter("f_apply", "Apply+Filter")
    uri.appendQueryParameter("f_search", (query + " " + EHentaiQuery.combine(filters)).trim())
    filters.forEach {
        if (it is UriFilter) it.addToUri(uri)
    }
    // Reverse search results on filter
    if (isReverseFilterEnabled) {
        uri.appendQueryParameter(REVERSE_PARAM, "on")
    }
    if (jumpSeekValue != null && page == 1) {
        uri.appendJumpOrSeek(jumpSeekValue)
    }

    return exGet(
        url = uri.toString(),
        next = if (!isReverseFilterEnabled) page else null,
        prev = if (isReverseFilterEnabled) page else null,
    )
}
