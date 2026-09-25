package exh.md.handlers

internal fun preferences(
    altTitlesInDesc: Boolean = false,
    finalChapterInDesc: Boolean = false,
    tryUsingFirstVolumeCover: Boolean = false,
    preferExtensionLangTitle: Boolean = true,
): MangaDetailsPreferences = MangaDetailsPreferences(
    coverQuality = ".512.jpg",
    tryUsingFirstVolumeCover = tryUsingFirstVolumeCover,
    altTitlesInDesc = altTitlesInDesc,
    finalChapterInDesc = finalChapterInDesc,
    preferExtensionLangTitle = preferExtensionLangTitle,
)
