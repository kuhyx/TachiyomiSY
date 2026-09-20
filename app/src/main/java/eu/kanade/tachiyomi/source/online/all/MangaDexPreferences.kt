package eu.kanade.tachiyomi.source.online.all

import eu.kanade.tachiyomi.source.online.all.MangaDex.Companion.getAltTitlesInDescKey
import eu.kanade.tachiyomi.source.online.all.MangaDex.Companion.getBlockedGroupsPrefKey
import eu.kanade.tachiyomi.source.online.all.MangaDex.Companion.getBlockedUploaderPrefKey
import eu.kanade.tachiyomi.source.online.all.MangaDex.Companion.getCoverQualityPrefKey
import eu.kanade.tachiyomi.source.online.all.MangaDex.Companion.getDataSaverPreferenceKey
import eu.kanade.tachiyomi.source.online.all.MangaDex.Companion.getFinalChapterInDescPrefKey
import eu.kanade.tachiyomi.source.online.all.MangaDex.Companion.getStandardHttpsPreferenceKey
import eu.kanade.tachiyomi.source.online.all.MangaDex.Companion.getTryUsingFirstVolumeCoverKey
import eu.kanade.tachiyomi.source.online.all.MangaDex.Companion.preferExtensionLangTitleKey
import exh.md.handlers.MangaDetailsPreferences
import uy.kohesive.injekt.api.get

internal fun MangaDex.dataSaver() = sourcePreferences.getBoolean(getDataSaverPreferenceKey(mdLang.lang), false)

internal fun MangaDex.usePort443Only() = sourcePreferences.getBoolean(getStandardHttpsPreferenceKey(mdLang.lang), false)

internal fun MangaDex.blockedGroups() = sourcePreferences.getString(getBlockedGroupsPrefKey(mdLang.lang), "").orEmpty()

internal fun MangaDex.blockedUploaders() =
    sourcePreferences.getString(getBlockedUploaderPrefKey(mdLang.lang), "").orEmpty()

internal fun MangaDex.coverQuality() = sourcePreferences.getString(getCoverQualityPrefKey(mdLang.lang), "").orEmpty()

internal fun MangaDex.tryUsingFirstVolumeCover() =
    sourcePreferences.getBoolean(getTryUsingFirstVolumeCoverKey(mdLang.lang), false)

internal fun MangaDex.altTitlesInDesc() = sourcePreferences.getBoolean(getAltTitlesInDescKey(mdLang.lang), false)

internal fun MangaDex.finalChapterInDesc() =
    sourcePreferences.getBoolean(getFinalChapterInDescPrefKey(mdLang.lang), false)

internal fun MangaDex.preferExtensionLangTitle() =
    sourcePreferences.getBoolean(preferExtensionLangTitleKey(mdLang.extLang), true)

internal fun MangaDex.detailsPreferences() = MangaDetailsPreferences(
    coverQuality = coverQuality(),
    tryUsingFirstVolumeCover = tryUsingFirstVolumeCover(),
    altTitlesInDesc = altTitlesInDesc(),
    finalChapterInDesc = finalChapterInDesc(),
    preferExtensionLangTitle = preferExtensionLangTitle(),
)
