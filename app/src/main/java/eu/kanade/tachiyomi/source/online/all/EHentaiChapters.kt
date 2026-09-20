package eu.kanade.tachiyomi.source.online.all

import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.asJsoup
import exh.debug.DebugToggles
import exh.eh.GalleryEntry
import exh.log.xLogD
import exh.metadata.MetadataUtil
import exh.metadata.metadata.EHentaiSearchMetadata
import kotlinx.serialization.json.put
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import java.time.ZoneOffset
import java.time.ZonedDateTime

internal suspend fun EHentai.getChapterList(manga: SManga): List<SChapter> = getChapterList(manga) {}

internal suspend fun EHentai.getChapterList(manga: SManga, throttleFunc: suspend () -> Unit): List<SChapter> {
    val doc = rootGalleryPage(manga.url, throttleFunc)
    // Build chapter for root gallery
    val location = doc.location()
    val self = SChapter(
        url = EHentaiSearchMetadata.normalizeUrl(location),
        name = "v1: " + doc.selectFirst("#gn")!!.text(),
        chapterNumber = 1f,
        dateUpload = parseExDate(doc.galleryDetail("posted:")!!.text()),
        scanlator = EHentaiSearchMetadata.galleryId(location),
    )
    // Build and append the rest of the galleries
    if (DebugToggles.INCLUDE_ONLY_ROOT_WHEN_LOADING_EXH_VERSIONS.enabled) return listOf(self)
    val newDisplay = doc.select("#gnd a")
    return newDisplay.mapIndexed { index, newGallery ->
        val link = newGallery.attr("href")
        val posted = (newGallery.nextSibling() as TextNode).text().removePrefix(", added ")
        SChapter(
            url = EHentaiSearchMetadata.normalizeUrl(link),
            name = "v${index + 2}: ${newGallery.text()}",
            chapterNumber = index + 2f,
            dateUpload = parseExDate(posted),
            scanlator = EHentaiSearchMetadata.galleryId(link),
        )
    }.reversed() + self
}

// Pull all the way to the root gallery.
// We can't do this with RxJava or we run into stack overflows on shit like this:
//   https://exhentai.org/g/1073061/f9345f1c12/
private suspend fun EHentai.rootGalleryPage(startUrl: String, throttleFunc: suspend () -> Unit): Document {
    var url = startUrl
    while (true) {
        val gid = EHentaiSearchMetadata.galleryId(url).toInt()
        val cachedParent = updateHelper.parentLookupTable.get(gid)
        if (cachedParent != null) {
            xLogD("Parent cache hit: %s!", gid)
            url = EHentaiSearchMetadata.idAndTokenToUrl(cachedParent.gId, cachedParent.gToken)
            continue
        }
        throttleFunc()
        val doc = client.newCall(exGet(baseUrl + url)).awaitSuccess().asJsoup()
        val parentLink = doc.galleryDetail("parent:")!!.selectFirst("a")?.attr("href") ?: return doc
        updateHelper.parentLookupTable.put(
            gid,
            GalleryEntry(EHentaiSearchMetadata.galleryId(parentLink), EHentaiSearchMetadata.galleryToken(parentLink)),
        )
        url = EHentaiSearchMetadata.normalizeUrl(parentLink)
    }
}

// The value cell next to the `#gdd` label [label] (e.g. "posted:"), if the page has it.
private fun Document.galleryDetail(label: String): Element? =
    select("#gdd .gdt1").find { el -> el.text().lowercase() == label }!!.nextElementSibling()

private fun parseExDate(text: String): Long =
    ZonedDateTime.parse(text, MetadataUtil.EX_DATE_FORMAT.withZone(ZoneOffset.UTC))!!.toInstant().toEpochMilli()

internal fun EHentai.parseChapterPage(response: Element) = with(response) {
    select(".gdtm a").map {
        Pair(it.child(0).attr("alt").toInt(), it.attr("href"))
    }.plus(
        select("#gdt a").map {
            Pair(it.child(0).attr("title").removePrefix("Page ").substringBefore(":").toInt(), it.attr("href"))
        },
    ).sortedBy(Pair<Int, String>::first).map { it.second }
}

internal fun EHentai.chapterPageRequest(np: String): Request = exGet(url = np, additionalHeaders = headers)

internal fun EHentai.nextPageUrl(element: Element): String? = element.select("a[onclick=return false]").last()?.let {
    return if (it.text() == ">") it.attr("href") else null
}
