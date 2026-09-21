package exh.util

import android.content.Context
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.sy.SYMR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Locale

internal fun Manga.mangaType(context: Context): String {
    return context.stringResource(
        when (mangaType()) {
            MangaType.TYPE_WEBTOON -> SYMR.strings.entry_type_webtoon
            MangaType.TYPE_MANHWA -> SYMR.strings.entry_type_manhwa
            MangaType.TYPE_MANHUA -> SYMR.strings.entry_type_manhua
            MangaType.TYPE_COMIC -> SYMR.strings.entry_type_comic
            else -> SYMR.strings.entry_type_manga
        },
    ).lowercase(Locale.getDefault())
}

/**
 * The type of comic the manga is (ie. manga, manhwa, manhua)
 */
internal fun Manga.mangaType(sourceName: String? = Injekt.get<SourceManager>().get(source)?.name): MangaType {
    val currentTags = genre.orEmpty()
    if (currentTags.any { tag -> isMangaTag(tag) }) return MangaType.TYPE_MANGA
    val match = TYPE_DETECTORS.firstOrNull { (_, isTag, isSource) ->
        currentTags.any(isTag) || sourceName?.let(isSource) == true
    }
    return match?.type ?: MangaType.TYPE_MANGA
}

private data class TypeDetector(
    val type: MangaType,
    val isTag: (String) -> Boolean,
    val isSource: (String) -> Boolean,
)

// Checked in order; an explicit "manga" tag wins over all of these.
private val TYPE_DETECTORS = listOf(
    TypeDetector(MangaType.TYPE_WEBTOON, ::isWebtoonTag, ::isWebtoonSource),
    TypeDetector(MangaType.TYPE_COMIC, ::isComicTag, ::isComicSource),
    TypeDetector(MangaType.TYPE_MANHUA, ::isManhuaTag, ::isManhuaSource),
    TypeDetector(MangaType.TYPE_MANHWA, ::isManhwaTag, ::isManhwaSource),
)

/**
 * The type the reader should use. Different from manga type as certain manga has different
 * read types
 */
internal fun Manga.defaultReaderType(type: MangaType = mangaType()): Int? {
    return if (type == MangaType.TYPE_MANHWA || type == MangaType.TYPE_WEBTOON) {
        ReadingMode.WEBTOON.flagValue
    } else {
        null
    }
}

/*private fun isMangaSource(sourceName: String): Boolean {
    return
}*/

// Case-insensitive substrings of source names, one list per comic type.
private val MANHWA_SOURCES = listOf(
    "hiperdex", "hmanhwa", "instamanhwa", "manhwa18", "manhwa68", "manhwa365", "manhwahentaime",
    "manhwamanga", "manhwatop", "manhwa club", "manytoon", "manwha", "readmanhwa", "skymanga",
    "toonily", "webtoonxyz",
)

// "tapas" deliberately left out: it hosts more than webtoons.
private val WEBTOON_SOURCES = listOf("mangatoon", "manmanga", "toomics", "webcomics", "webtoons", "webtoon")

private val COMIC_SOURCES = listOf(
    "8muses", "allporncomic", "ciayo comics", "comicextra", "comicpunch", "cyanide", "dilbert",
    "eggporncomics", "existential comics", "hiveworks comics", "milftoon", "myhentaicomics",
    "myhentaigallery", "gunnerkrigg", "oglaf", "patch friday", "porncomix", "questionable content",
    "readcomiconline", "read comics online", "swords comic", "teabeer comics", "xkcd",
)

private val MANHUA_SOURCES = listOf(
    "1st kiss manhua", "hero manhua", "manhuabox", "manhuaus", "manhuas world", "manhuas.net",
    "readmanhua", "wuxiaworld", "manhua",
)

private fun String.matchesAny(needles: List<String>): Boolean = needles.any { contains(it, ignoreCase = true) }

private fun isManhwaSource(sourceName: String): Boolean = sourceName.matchesAny(MANHWA_SOURCES)

private fun isWebtoonSource(sourceName: String): Boolean = sourceName.matchesAny(WEBTOON_SOURCES)

private fun isComicSource(sourceName: String): Boolean = sourceName.matchesAny(COMIC_SOURCES)

private fun isManhuaSource(sourceName: String): Boolean = sourceName.matchesAny(MANHUA_SOURCES)

internal enum class MangaType {
    TYPE_MANGA,
    TYPE_MANHWA,
    TYPE_MANHUA,
    TYPE_COMIC,
    TYPE_WEBTOON,
}
