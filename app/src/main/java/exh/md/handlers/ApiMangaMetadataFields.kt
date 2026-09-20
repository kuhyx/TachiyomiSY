package exh.md.handlers

import eu.kanade.tachiyomi.source.model.SManga
import exh.md.dto.MangaAttributesDto
import exh.md.dto.MangaDataDto
import exh.md.utils.MdConstants
import exh.md.utils.MdUtil
import exh.md.utils.addAltTitleToDesc
import exh.md.utils.addFinalChapterToDesc
import exh.md.utils.asMdMap
import exh.md.utils.cleanDescription
import exh.md.utils.getFromLangMap
import exh.metadata.metadata.MangaDexSearchMetadata
import exh.metadata.metadata.base.RaisedTag
import exh.util.capitalize
import java.util.Locale

// The individual fields [ApiMangaParser.parseIntoMetadata] derives from a MangaDex manga response.

/** The explicit cover file when the caller resolved one, else the cover-art relationship's. */
internal fun MangaDataDto.coverUrl(coverFileName: String?, coverQuality: String): String? {
    val fileName = coverFileName?.takeIf { it.isNotEmpty() }
        ?: relationships
            .firstOrNull { relationshipDto -> relationshipDto.type == MdConstants.Types.coverArt }
            ?.attributes
            ?.fileName
        ?: return null
    return MdUtil.cdnCoverUrl(id, "$fileName$coverQuality")
}

internal fun MangaAttributesDto.description(
    lang: String,
    altTitles: List<String>?,
    preferences: MangaDetailsPreferences,
): String {
    val rawDesc = MdUtil.getFromLangMap(
        langMap = description.asMdMap(),
        currentLang = lang,
        originalLanguage = originalLanguage,
    ).orEmpty()
    return MdUtil.cleanDescription(rawDesc)
        .let { if (preferences.altTitlesInDesc) MdUtil.addAltTitleToDesc(it, altTitles) else it }
        .let { if (preferences.finalChapterInDesc) MdUtil.addFinalChapterToDesc(it, lastVolume, lastChapter) else it }
}

/** The names of every relationship of [type], e.g. the authors or the artists. */
internal fun MangaDataDto.relationshipNames(type: String): List<String> =
    relationships.filter { it.type.equals(type, true) }.mapNotNull { it.attributes?.name }.distinct()

/** A finished or cancelled series whose last chapter is present reads as completed. */
internal fun MangaAttributesDto.mangaStatus(parsedStatus: Int, simpleChapters: List<String>): Int {
    val publishedOrCancelled = parsedStatus == SManga.PUBLISHING_FINISHED || parsedStatus == SManga.CANCELLED
    return if (lastChapter != null && publishedOrCancelled && lastChapter in simpleChapters) {
        SManga.COMPLETED
    } else {
        parsedStatus
    }
}

/** Demographic and content rating go with the genre tags but aren't actually genres; then the real tags. */
internal fun MangaAttributesDto.genreTags(lang: String): List<RaisedTag> {
    val nonGenres = listOfNotNull(
        publicationDemographic?.let {
            RaisedTag("Demographic", it.capitalize(Locale.US), MangaDexSearchMetadata.TAG_TYPE_DEFAULT)
        },
        contentRating
            ?.takeUnless { it == "safe" }
            ?.let { RaisedTag("Content Rating", it.capitalize(Locale.US), MangaDexSearchMetadata.TAG_TYPE_DEFAULT) },
    )
    return nonGenres + tags
        .mapNotNull { it.attributes.name[lang] ?: it.attributes.name["en"] }
        .map { RaisedTag("Tags", it, MangaDexSearchMetadata.TAG_TYPE_DEFAULT) }
}

internal fun MangaDexSearchMetadata.applyExternalLinks(attributes: MangaAttributesDto) {
    attributes.links?.asMdMap<String>()?.let { links ->
        links["al"]?.let { anilistId = it }
        links["kt"]?.let { kitsuId = it }
        links["mal"]?.let { myAnimeListId = it }
        links["mu"]?.let { mangaUpdatesId = it }
        links["ap"]?.let { animePlanetId = it }
    }
}
