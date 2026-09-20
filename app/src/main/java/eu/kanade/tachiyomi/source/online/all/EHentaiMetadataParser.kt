package eu.kanade.tachiyomi.source.online.all

import exh.eh.EHentaiUpdateWorkerConstants
import exh.log.xLogD
import exh.metadata.MetadataUtil
import exh.metadata.metadata.EHentaiSearchMetadata
import exh.metadata.metadata.EHentaiSearchMetadata.Companion.EH_GENRE_NAMESPACE
import exh.metadata.metadata.EHentaiSearchMetadata.Companion.EH_META_NAMESPACE
import exh.metadata.metadata.EHentaiSearchMetadata.Companion.EH_UPLOADER_NAMESPACE
import exh.metadata.metadata.EHentaiSearchMetadata.Companion.EH_VISIBILITY_NAMESPACE
import exh.metadata.metadata.EHentaiSearchMetadata.Companion.TAG_TYPE_LIGHT
import exh.metadata.metadata.EHentaiSearchMetadata.Companion.TAG_TYPE_NORMAL
import exh.metadata.metadata.EHentaiSearchMetadata.Companion.TAG_TYPE_WEAK
import exh.metadata.metadata.RaisedSearchMetadata.Companion.TAG_TYPE_VIRTUAL
import exh.metadata.metadata.base.RaisedTag
import exh.util.ignore
import exh.util.nullIfBlank
import exh.util.trimOrNull
import org.jsoup.nodes.Document
import java.time.ZoneOffset
import java.time.ZonedDateTime

private const val TR_SUFFIX = "TR"

/** Fills an [EHentaiSearchMetadata] from a gallery page: titles, tags, uploader, rating, size. */
internal class EHentaiMetadataParser(private val exh: Boolean) {
    fun parseInto(metadata: EHentaiSearchMetadata, input: Document) {
        with(metadata) {
            with(input) {
                val url = location()
                gId = EHentaiSearchMetadata.galleryId(url)
                gToken = EHentaiSearchMetadata.galleryToken(url)

                exh = this@EHentaiMetadataParser.exh
                title = select("#gn").text().trimOrNull()

                altTitle = select("#gj").text().trimOrNull()

                thumbnailUrl = select("#gd1 div").attr("style").nullIfBlank()?.let {
                    it.substring(it.indexOf('(') + 1 until it.lastIndexOf(')'))
                }
                genre = select(".cs")
                    .attr("onclick")
                    .trimOrNull()
                    ?.substringAfterLast('/')
                    ?.removeSuffix("'")

                uploader = select("#gdn").text().trimOrNull()

                // Parse the table
                select("#gdd tr").forEach {
                    val left = it.select(".gdt1").text().trimOrNull()
                    val rightElement = it.selectFirst(".gdt2")!!
                    val right = rightElement.text().trimOrNull()
                    if (left != null && right != null) {
                        ignore {
                            when (left.removeSuffix(":").lowercase()) {
                                "posted" -> {
                                    datePosted = ZonedDateTime.parse(
                                        right,
                                        MetadataUtil.EX_DATE_FORMAT.withZone(ZoneOffset.UTC),
                                    ).toInstant().toEpochMilli()
                                }
                                // Example gallery with parent: https://e-hentai.org/g/1390451/7f181c2426/
                                // Example JP gallery: https://exhentai.org/g/1375385/03519d541b/
                                // Parent is older variation of the gallery
                                "parent" -> {
                                    parent = if (!right.equals("None", true)) {
                                        rightElement.child(0).attr("href")
                                    } else {
                                        null
                                    }
                                }
                                "visible" -> {
                                    visible = right.nullIfBlank()
                                }
                                "language" -> {
                                    language = right.removeSuffix(TR_SUFFIX).trimOrNull()
                                    translated = right.endsWith(TR_SUFFIX, true)
                                }
                                "file size" -> {
                                    size = MetadataUtil.parseHumanReadableByteCount(right)?.toLong()
                                }
                                "length" -> {
                                    length = right.removeSuffix("pages").trimOrNull()?.toInt()
                                }
                                "favorited" -> {
                                    favorites = right.removeSuffix("times").trimOrNull()?.toInt()
                                }
                            }
                        }
                    }
                }

                lastUpdateCheck = System.currentTimeMillis()
                if (datePosted != null &&
                    lastUpdateCheck - datePosted!! > EHentaiUpdateWorkerConstants.GALLERY_AGE_TIME
                ) {
                    aged = true
                    this@EHentaiMetadataParser.xLogD("aged %s - too old", title)
                }

                // Parse ratings
                ignore {
                    averageRating = select("#rating_label")
                        .text()
                        .removePrefix("Average:")
                        .trimOrNull()
                        ?.toDouble()
                    ratingCount = select("#rating_count")
                        .text()
                        .trimOrNull()
                        ?.toInt()
                }

                // Parse tags
                tags.clear()
                select("#taglist tr").forEach {
                    val namespace = it.select(".tc").text().removeSuffix(":")
                    tags += it.select("div").map { element ->
                        RaisedTag(
                            namespace,
                            element.text().trim(),
                            when {
                                element.hasClass("gtl") -> TAG_TYPE_LIGHT
                                element.hasClass("gtw") -> TAG_TYPE_WEAK
                                else -> TAG_TYPE_NORMAL
                            },
                        )
                    }
                }

                // Add genre as virtual tag
                genre?.let {
                    tags += RaisedTag(EH_GENRE_NAMESPACE, it, TAG_TYPE_VIRTUAL)
                }
                if (aged) {
                    tags += RaisedTag(EH_META_NAMESPACE, "aged", TAG_TYPE_VIRTUAL)
                }
                uploader?.let {
                    tags += RaisedTag(EH_UPLOADER_NAMESPACE, it, TAG_TYPE_VIRTUAL)
                }
                visible?.let {
                    tags += RaisedTag(
                        EH_VISIBILITY_NAMESPACE,
                        it.substringAfter('(').substringBeforeLast(')'),
                        TAG_TYPE_VIRTUAL,
                    )
                }
            }
        }
    }
}
