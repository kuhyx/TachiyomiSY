package exh.md.utils

import android.app.Application
import exh.md.dto.MangaAttributesDto
import org.jsoup.parser.Parser
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.sy.SYMR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal fun MdUtil.cleanDescription(string: String): String {
    return Parser.unescapeEntities(string, false)
        .substringBefore("\n---")
        .replace(markdownLinksRegex, "$1")
        .replace(markdownItalicBoldRegex, "$1")
        .replace(markdownItalicRegex, "$1")
        .trim()
}

internal fun MdUtil.getTitleFromManga(
    json: MangaAttributesDto,
    lang: String,
    preferExtensionLangTitle: Boolean,
): String {
    val titleMap = json.title.asMdMap<String>()
    val altTitles = json.altTitles
    val originalLang = json.originalLanguage

    titleMap[lang]?.let { return it }

    val mainTitle = titleMap.values.firstOrNull()
    val langTitle = findTitleInMaps(lang, titleMap, altTitles)
    val enTitle = findTitleInMaps("en", titleMap, altTitles)
    val originalLangTitle = findTitleInMaps("$originalLang-ro", titleMap, altTitles) ?: findTitleInMaps(
        originalLang,
        titleMap,
        altTitles,
    )

    val ordered = if (preferExtensionLangTitle) {
        listOf(langTitle, mainTitle, enTitle, originalLangTitle)
    } else {
        listOf(mainTitle, langTitle, enTitle, originalLangTitle)
    }

    return ordered.firstOrNull { it != null }
        ?: ""
}

internal fun MdUtil.getFromLangMap(
    langMap: Map<String, String>,
    currentLang: String,
    originalLanguage: String,
): String? =
    langMap[currentLang]
        ?: langMap["en"]
        ?: if (originalLanguage == "ja") {
            langMap["ja-ro"]
                ?: langMap["jp-ro"]
        } else {
            null
        }

internal fun MdUtil.findTitleInMaps(
    lang: String,
    titleMap: Map<String, String>,
    altTitleMaps: List<Map<String, String>>,
): String? = titleMap[lang] ?: altTitleMaps.firstNotNullOfOrNull { it[lang] }

internal fun MdUtil.addAltTitleToDesc(description: String, altTitles: List<String>?): String {
    return if (altTitles.isNullOrEmpty()) {
        description
    } else {
        val altTitlesDesc = altTitles
            .joinToString(
                "\n",
                "${Injekt.get<Application>().stringResource(SYMR.strings.alt_titles)}:\n",
            ) { "• $it" }
        description + (if (description.isBlank()) "" else "\n\n") + Parser.unescapeEntities(
            altTitlesDesc,
            false,
        )
    }
}

internal fun MdUtil.addFinalChapterToDesc(description: String, lastVolume: String?, lastChapter: String?): String {
    val parts = listOfNotNull(
        lastVolume?.takeIf { it.isNotEmpty() }?.let { "Vol.$it" },
        lastChapter?.takeIf { it.isNotEmpty() }?.let { "Ch.$it" },
    )

    return if (parts.isEmpty()) {
        description
    } else {
        description + (if (description.isBlank()) "" else "\n\n") + parts.joinToString(
            " ",
            "${Injekt.get<Application>().stringResource(SYMR.strings.final_chapter)}:\n",
        )
    }
}
