package mihon.core.migration.migrations

import exh.source.MERGED_SOURCE_ID
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga

/** A merged-source manga whose url is the legacy JSON config listing its [children] as `source to url`. */
internal fun mergedManga(id: Long, children: List<Pair<Long, String>>): Manga {
    val entries = children.joinToString(",") { (source, url) -> """{"s":$source,"u":"$url"}""" }
    return Manga.create().copy(id = id, source = MERGED_SOURCE_ID, url = """{"c":[$entries]}""")
}

/** A manga at [url] on [source]. */
internal fun sourceManga(id: Long, source: Long, url: String): Manga =
    Manga.create().copy(id = id, source = source, url = url)

/** A chapter of the merged manga whose url carries the legacy `source/url/mangaUrl` triple. */
internal fun legacyChapter(
    id: Long,
    read: Boolean,
    lastPageRead: Long = 0,
    url: String,
): Chapter = Chapter.create().copy(id = id, read = read, lastPageRead = lastPageRead, url = url)

/** The legacy chapter-url JSON for [source], [url] and [mangaUrl]. */
internal fun legacyChapterUrl(source: Long, url: String, mangaUrl: String): String =
    """{"s":$source,"u":"$url","m":"$mangaUrl"}"""
