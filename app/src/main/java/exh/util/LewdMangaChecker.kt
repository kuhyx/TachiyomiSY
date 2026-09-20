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
        return genre.orEmpty().none { tag -> isNonHentaiTag(tag) }
    }

    return source in LEWD_SOURCE_SERIES + FIRST_LEWD_OFFSET..LEWD_SOURCE_SERIES + LAST_LEWD_OFFSET ||
        (sourceName != null && isHentaiSource(sourceName)) ||
        genre.orEmpty().any { tag -> isHentaiTag(tag) }
}

private fun isNonHentaiTag(tag: String): Boolean = tag.contains("non-h", true)

private fun isHentaiTag(tag: String): Boolean {
    return tag.contains("hentai", true) ||
        tag.contains("adult", true) ||
        tag.contains("smut", true) ||
        tag.contains("lewd", true) ||
        tag.contains("nsfw", true) ||
        tag.contains("erotica", true) ||
        tag.contains("pornographic", true) ||
        tag.contains("mature", true) ||
        tag.contains("18+", true)
}

private fun isHentaiSource(source: String): Boolean {
    return source.contains("allporncomic", true) ||
        source.contains("hentai cafe", true) ||
        source.contains("hentai2read", true) ||
        source.contains("hentaifox", true) ||
        source.contains("hentainexus", true) ||
        source.contains("manhwahentai.me", true) ||
        source.contains("milftoon", true) ||
        source.contains("myhentaicomics", true) ||
        source.contains("myhentaigallery", true) ||
        source.contains("ninehentai", true) ||
        source.contains("pururin", true) ||
        source.contains("simply hentai", true) ||
        source.contains("tsumino", true) ||
        source.contains("8muses", true) ||
        source.contains("hbrowse", true) ||
        source.contains("nhentai", true) ||
        source.contains("erofus", true) ||
        source.contains("luscious", true) ||
        source.contains("doujins", true) ||
        source.contains("multporn", true) ||
        source.contains("vcp", true) ||
        source.contains("vmp", true) ||
        source.contains("hentai", true)
}
