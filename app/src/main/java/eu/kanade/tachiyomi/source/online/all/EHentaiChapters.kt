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
    // Pull all the way to the root gallery
    // We can't do this with RxJava or we run into stack overflows on shit like this:
    //   https://exhentai.org/g/1073061/f9345f1c12/
    var url = manga.url
    var doc: Document

    while (true) {
        val gid = EHentaiSearchMetadata.galleryId(url).toInt()
        val cachedParent = updateHelper.parentLookupTable.get(
            gid,
        )
        if (cachedParent == null) {
            throttleFunc()
            doc = client.newCall(exGet(baseUrl + url)).awaitSuccess().asJsoup()

            val parentLink = doc.select("#gdd .gdt1").find { el ->
                el.text().lowercase() == "parent:"
            }!!.nextElementSibling()!!.selectFirst("a")?.attr("href")

            if (parentLink != null) {
                updateHelper.parentLookupTable.put(
                    gid,
                    GalleryEntry(
                        EHentaiSearchMetadata.galleryId(parentLink),
                        EHentaiSearchMetadata.galleryToken(parentLink),
                    ),
                )
                url = EHentaiSearchMetadata.normalizeUrl(parentLink)
            } else {
                break
            }
        } else {
            this@getChapterList.xLogD("Parent cache hit: %s!", gid)
            url = EHentaiSearchMetadata.idAndTokenToUrl(
                cachedParent.gId,
                cachedParent.gToken,
            )
        }
    }
    val newDisplay = doc.select("#gnd a")
    // Build chapter for root gallery
    val location = doc.location()
    val self = SChapter(
        url = EHentaiSearchMetadata.normalizeUrl(location),
        name = "v1: " + doc.selectFirst("#gn")!!.text(),
        chapterNumber = 1f,
        dateUpload = ZonedDateTime.parse(
            doc.select("#gdd .gdt1").find { el ->
                el.text().lowercase() == "posted:"
            }!!.nextElementSibling()!!.text(),
            MetadataUtil.EX_DATE_FORMAT.withZone(ZoneOffset.UTC),
        )!!.toInstant().toEpochMilli(),
        scanlator = EHentaiSearchMetadata.galleryId(location),
    )
    // Build and append the rest of the galleries
    return if (DebugToggles.INCLUDE_ONLY_ROOT_WHEN_LOADING_EXH_VERSIONS.enabled) {
        listOf(self)
    } else {
        newDisplay.mapIndexed { index, newGallery ->
            val link = newGallery.attr("href")
            val name = newGallery.text()
            val posted = (newGallery.nextSibling() as TextNode).text().removePrefix(", added ")
            SChapter(
                url = EHentaiSearchMetadata.normalizeUrl(link),
                name = "v${index + 2}: $name",
                chapterNumber = index + 2f,
                dateUpload = ZonedDateTime.parse(
                    posted,
                    MetadataUtil.EX_DATE_FORMAT.withZone(ZoneOffset.UTC),
                ).toInstant().toEpochMilli(),
                scanlator = EHentaiSearchMetadata.galleryId(link),
            )
        }.reversed() + self
    }
}

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
