package tachiyomi.source.local.metadata

import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.storage.EpubFile
import org.jsoup.nodes.Element
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Fills manga and chapter metadata using this epub file's metadata: the creator becomes the
 * author, the description the summary, the title the chapter name, the publisher (or else the
 * creator) the scanlator and the modification date the upload date.
 */
public fun EpubFile.fillMetadata(manga: SManga, chapter: SChapter) {
    val ref = getPackageHref()
    val doc = getPackageDocument(ref)

    val title: Element? = doc.getElementsByTag("dc:title").first()
    val publisher: Element? = doc.getElementsByTag("dc:publisher").first()
    val creator: Element? = doc.getElementsByTag("dc:creator").first()
    val description: Element? = doc.getElementsByTag("dc:description").first()
    val date: Element? = doc.getElementsByTag("dc:date").first()
        ?: doc.select("meta[property=dcterms:modified]").first()

    creator?.let { manga.author = it.text() }
    description?.let { manga.description = it.text() }

    title?.let { chapter.name = it.text() }

    if (publisher != null) {
        chapter.scanlator = publisher.text()
    } else if (creator != null) {
        chapter.scanlator = creator.text()
    }

    date?.let { chapter.date_upload = parseDate(it.text()) ?: chapter.date_upload }
}

private val EpubDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ")

private fun parseDate(text: String): Long? = try {
    OffsetDateTime.parse(text, EpubDateFormat).toInstant().toEpochMilli()
} catch (_: DateTimeParseException) {
    null
}
