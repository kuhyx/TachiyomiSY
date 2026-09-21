package exh.util

import exh.source.LEWD_SOURCE_SERIES
import exh.source.isEhBasedManga
import exh.source.nHentaiSourceIds
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

// The built-in adult sources took LEWD_SOURCE_SERIES + 5 .. + 13.
private const val FIRST_LEWD_OFFSET = 5L
private const val LAST_LEWD_OFFSET = 13L

internal fun Manga.isLewd(): Boolean {
    val sourceName = Injekt.get<SourceManager>().get(source)?.name

    if (isEhBasedManga() || source in nHentaiSourceIds) {
        return genre.orEmpty().all { tag -> !isNonHentaiTag(tag) }
    }

    return source in LEWD_SOURCE_SERIES + FIRST_LEWD_OFFSET..LEWD_SOURCE_SERIES + LAST_LEWD_OFFSET ||
        (sourceName != null && isHentaiSource(sourceName)) ||
        genre.orEmpty().any { tag -> isHentaiTag(tag) }
}

private fun isNonHentaiTag(tag: String): Boolean = tag.contains("non-h", true)

// Case-insensitive substrings that mark a genre tag / source name as adult.
private val HENTAI_TAGS = listOf(
    "hentai", "adult", "smut", "lewd", "nsfw", "erotica", "pornographic", "mature", "18+",
)

private val HENTAI_SOURCES = listOf(
    "allporncomic", "hentai cafe", "hentai2read", "hentaifox", "hentainexus", "manhwahentai.me",
    "milftoon", "myhentaicomics", "myhentaigallery", "ninehentai", "pururin", "simply hentai",
    "tsumino", "8muses", "hbrowse", "nhentai", "erofus", "luscious", "doujins", "multporn",
    "vcp", "vmp", "hentai",
)

private fun isHentaiTag(tag: String): Boolean = HENTAI_TAGS.any { tag.contains(it, ignoreCase = true) }

private fun isHentaiSource(source: String): Boolean = HENTAI_SOURCES.any { source.contains(it, ignoreCase = true) }
