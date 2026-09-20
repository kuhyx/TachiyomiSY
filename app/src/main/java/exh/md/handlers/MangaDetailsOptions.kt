package exh.md.handlers

import exh.md.dto.StatisticsMangaDto

/** The MangaDex source preferences that shape a manga's details. */
internal data class MangaDetailsPreferences(
    val coverQuality: String,
    val tryUsingFirstVolumeCover: Boolean,
    val altTitlesInDesc: Boolean,
    val finalChapterInDesc: Boolean,
    val preferExtensionLangTitle: Boolean,
)

/** The extra lookups fetched alongside a manga, each optional on failure. */
internal data class MangaDetailsExtras(
    val simpleChapters: List<String>,
    val statistics: StatisticsMangaDto?,
    val coverFileName: String?,
)
