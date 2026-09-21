package eu.kanade.tachiyomi.source.online.all

import exh.md.handlers.MangaDetailsPreferences
import uy.kohesive.injekt.api.get

internal fun MangaDex.dataSaver() = sourcePreferences.getBoolean(MangaDex.getDataSaverPreferenceKey(mdLang.lang), false)

internal fun MangaDex.usePort443Only() =
    sourcePreferences.getBoolean(MangaDex.getStandardHttpsPreferenceKey(mdLang.lang), false)

internal fun MangaDex.blockedGroups() =
    sourcePreferences.getString(MangaDex.getBlockedGroupsPrefKey(mdLang.lang), "").orEmpty()

internal fun MangaDex.blockedUploaders() =
    sourcePreferences.getString(MangaDex.getBlockedUploaderPrefKey(mdLang.lang), "").orEmpty()

internal fun MangaDex.coverQuality() =
    sourcePreferences.getString(MangaDex.getCoverQualityPrefKey(mdLang.lang), "").orEmpty()

internal fun MangaDex.tryUsingFirstVolumeCover() =
    sourcePreferences.getBoolean(MangaDex.getTryUsingFirstVolumeCoverKey(mdLang.lang), false)

internal fun MangaDex.altTitlesInDesc() =
    sourcePreferences.getBoolean(MangaDex.getAltTitlesInDescKey(mdLang.lang), false)

internal fun MangaDex.finalChapterInDesc() =
    sourcePreferences.getBoolean(MangaDex.getFinalChapterInDescPrefKey(mdLang.lang), false)

internal fun MangaDex.preferExtensionLangTitle() =
    sourcePreferences.getBoolean(MangaDex.preferExtensionLangTitleKey(mdLang.extLang), true)

internal fun MangaDex.detailsPreferences() = MangaDetailsPreferences(
    coverQuality = coverQuality(),
    tryUsingFirstVolumeCover = tryUsingFirstVolumeCover(),
    altTitlesInDesc = altTitlesInDesc(),
    finalChapterInDesc = finalChapterInDesc(),
    preferExtensionLangTitle = preferExtensionLangTitle(),
)
