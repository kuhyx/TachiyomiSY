package eu.kanade.tachiyomi.source.online.all

import eu.kanade.tachiyomi.source.model.SManga
import exh.metadata.MetadataUtil
import exh.metadata.metadata.EHentaiSearchMetadata
import exh.metadata.metadata.EHentaiSearchMetadata.Companion.TAG_TYPE_LIGHT
import exh.metadata.metadata.EHentaiSearchMetadata.Companion.TAG_TYPE_NORMAL
import exh.metadata.metadata.EHentaiSearchMetadata.Companion.TAG_TYPE_WEAK
import exh.metadata.metadata.RaisedSearchMetadata.Companion.toGenreString
import exh.metadata.metadata.base.RaisedTag
import exh.util.nullIfBlank
import exh.util.trimOrNull
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.IOException
import java.time.ZoneOffset
import java.time.ZonedDateTime

private const val TITLE = "title"
private const val STYLE = "style"

internal const val REVERSE_PARAM = "TEH_REVERSE"
private const val TOPLIST_LAST_PAGE = 200L
private const val BORDER_COLOR_START = 14
private const val BORDER_COLOR_END = 17
private const val COMPACT_RATING_INDEX = 3
private const val COMPACT_UPLOADER_INDEX = 4
private const val COMPACT_LENGTH_INDEX = 5
private const val EXTENDED_DATE_INDEX = 8
private const val EXTENDED_RATING_INDEX = 9
private const val MAX_RATING = 5
private const val STAR_SPRITE_WIDTH_PX = 16
private const val HALF_STAR_SPRITE_OFFSET_PX = 21
private const val HALF_STAR = 0.5

private val RATING_REGEX = "([0-9]*)px".toRegex()
private val FAVORITES_BORDER_HEX_COLORS = listOf(
    "000",
    "f00",
    "fa0",
    "dd0",
    "080",
    "9f4",
    "4bf",
    "00f",
    "508",
    "e8e",
)

/** Parses an e-hentai gallery list page (compact or extended layout) into entries and the next page. */
internal class EHentaiGalleryListParser {
    fun parse(doc: Document): Pair<List<EHentai.ParsedManga>, Long?> {
        // Parse mangas (supports compact + extended layout)
        val parsedMangas = doc.select(".itg > tbody > tr").filter { element ->
            // Do not parse header and ads
            element.selectFirst("th") == null && element.selectFirst(".itd") == null
        }.map(::parseRow).ifEmpty {
            doc.selectFirst(".searchwarn")?.let { throw IOException(it.text()) }
            emptyList()
        }

        val parsedLocation = doc.location().toHttpUrlOrNull()
        val isReversed = parsedLocation != null && parsedLocation.queryParameterNames.contains(REVERSE_PARAM)
        val nextPage = nextPage(doc, parsedLocation, parsedMangas, isReversed)
        return parsedMangas.let { if (isReversed) it.reversed() else it } to nextPage
    }

    private fun parseRow(body: Element): EHentai.ParsedManga {
        val thumbnailElement = body.selectFirst(".gl1e img, .gl2c .glthumb img")!!
        val column2 = body.selectFirst(".gl3e, .gl2c")!!
        val linkElement = body.selectFirst(".gl3c > a, .gl2e > div > a")!!
        val infoElements = body.selectFirst(".gl3e")?.select("div")

        // why is column2 null
        val favElement = column2.children().find { it.attr(STYLE).startsWith("border-color") }
        val parsedTags = parseTags(body, linkElement, isCompact = infoElements != null)

        return EHentai.ParsedManga(
            fav = FAVORITES_BORDER_HEX_COLORS.indexOf(
                favElement?.attr(STYLE)?.substring(BORDER_COLOR_START, BORDER_COLOR_END),
            ),
            manga = SManga.create().apply {
                // Get title
                title = thumbnailElement.attr(TITLE)
                url = EHentaiSearchMetadata.normalizeUrl(linkElement.attr("href"))
                // Get image
                thumbnail_url = thumbnailElement.attr("src")
                genre = parsedTags.toMutableList().toGenreString()
            },
            metadata = EHentaiSearchMetadata().apply {
                tags += parsedTags
                if (infoElements != null) parseCompactInfo(infoElements) else parseExtendedInfo(body)
            },
        )
    }

    // The compact layout lists tags by namespace row; the extended one flattens them into `.gt` divs.
    private fun parseTags(body: Element, linkElement: Element, isCompact: Boolean): List<RaisedTag> {
        if (!isCompact) {
            return body.selectFirst(".gl3c > a")!!.select("div")
                .filter { it.className() == "gt" }
                .map { element ->
                    val namespace = element.attr(TITLE).substringBefore(":").trimOrNull() ?: "misc"
                    RaisedTag(namespace, element.attr(TITLE).substringAfter(":").trim(), TAG_TYPE_NORMAL)
                }
        }
        return linkElement.select("div div").getOrNull(1)?.select("tr").orEmpty().flatMap { row ->
            val namespace = row.select(".tc").text().removeSuffix(":")
            row.select("div").map { element ->
                val type = when {
                    element.hasClass("gtl") -> TAG_TYPE_LIGHT
                    element.hasClass("gtw") -> TAG_TYPE_WEAK
                    else -> TAG_TYPE_NORMAL
                }
                RaisedTag(namespace, element.text().trim(), type)
            }
        }
    }

    private fun EHentaiSearchMetadata.parseCompactInfo(infoElements: List<Element>) {
        genre = getGenre(infoElements.getOrNull(1))
        datePosted = getDateTag(infoElements.getOrNull(2))
        averageRating = getRating(infoElements.getOrNull(COMPACT_RATING_INDEX))
        uploader = getUploader(infoElements.getOrNull(COMPACT_UPLOADER_INDEX))
        length = getPageCount(infoElements.getOrNull(COMPACT_LENGTH_INDEX))
    }

    private fun EHentaiSearchMetadata.parseExtendedInfo(body: Element) {
        genre = getGenre(body.selectFirst(".gl1c div"))
        val infoList = body.selectFirst(".gl2c")!!.select("div div")
        datePosted = getDateTag(infoList.getOrNull(EXTENDED_DATE_INDEX))
        averageRating = getRating(infoList.getOrNull(EXTENDED_RATING_INDEX))
        // The uploader column is absent on some listings, which shifts uploader and page count left by one.
        val extraInfoList = body.selectFirst(".gl4c")!!.select("div")
        val offset = if (extraInfoList.getOrNull(2) == null) 0 else 1
        uploader = getUploader(extraInfoList.getOrNull(offset))
        length = getPageCount(extraInfoList.getOrNull(offset + 1))
    }

    private fun getGenre(element: Element?): String? {
        return element?.attr("onclick")
            ?.nullIfBlank()
            ?.substringAfterLast('/')
            ?.removeSuffix("'")
            ?.trim()
            ?.substringAfterLast('/')
            ?.removeSuffix("'")
            ?: element?.text()
                ?.nullIfBlank()
                ?.lowercase()
                ?.replace(" ", "")
                ?.trim()
    }

    private fun getDateTag(element: Element?): Long? {
        val text = element?.text()?.nullIfBlank()
        return text?.let {
            ZonedDateTime.parse(it, MetadataUtil.EX_DATE_FORMAT.withZone(ZoneOffset.UTC)).toInstant().toEpochMilli()
        }
    }

    private fun getRating(element: Element?): Double? {
        val ratingStyle = element?.attr(STYLE)?.nullIfBlank()
        val matches = ratingStyle?.let { style ->
            // Group 1 always takes part in a match (possibly empty), so only the number can be missing.
            RATING_REGEX.findAll(style).mapNotNull { it.groupValues[1].toIntOrNull() }.toList()
        }
        if (matches == null || matches.size != 2) return null
        var rate = MAX_RATING - matches[0] / STAR_SPRITE_WIDTH_PX
        return if (matches[1] == HALF_STAR_SPRITE_OFFSET_PX) {
            rate--
            rate + HALF_STAR
        } else {
            rate.toDouble()
        }
    }

    private fun getUploader(element: Element?): String? = element?.select("a")?.text()?.trimOrNull()

    private fun getPageCount(element: Element?): Int? {
        val pageCount = element?.text()?.trimOrNull()
        // The leading ASCII digits, if any: what "[0-9]*" matched at the start of the text.
        return pageCount?.let { text -> text.takeWhile { it in '0'..'9' }.toIntOrNull() }
    }
}

// The next page's key: a page number on the toplist, otherwise the boundary gallery id.
internal fun nextPage(
    doc: Document,
    parsedLocation: HttpUrl?,
    parsedMangas: List<EHentai.ParsedManga>,
    isReversed: Boolean,
): Long? {
    val navDirection = if (isReversed) "prev" else "next"
    val hasNextPage = doc.select(".searchnav >div > a").any { navDirection in it.attr("href") }
    return when {
        parsedLocation?.pathSegments?.contains("toplist.php") == true ->
            ((parsedLocation.queryParameter("p")?.toLong() ?: 0) + 2).takeIf { it <= TOPLIST_LAST_PAGE }
        hasNextPage -> parsedMangas.let { if (isReversed) it.first() else it.last() }
            .manga
            .url
            .let { EHentaiSearchMetadata.galleryId(it).toLong() }
        else -> null
    }
}
