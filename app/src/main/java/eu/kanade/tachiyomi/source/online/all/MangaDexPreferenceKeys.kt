package eu.kanade.tachiyomi.source.online.all

import uy.kohesive.injekt.api.get

internal fun MangaDex.Companion.getDataSaverPreferenceKey(dexLang: String): String = "${dataSaverPref}_$dexLang"

internal fun MangaDex.Companion.getStandardHttpsPreferenceKey(
    dexLang: String,
): String = "${standardHttpsPortPref}_$dexLang"

internal fun MangaDex.Companion.getBlockedGroupsPrefKey(dexLang: String): String = "${blockedGroupsPref}_$dexLang"

internal fun MangaDex.Companion.getBlockedUploaderPrefKey(dexLang: String): String = "${blockedUploaderPref}_$dexLang"

internal fun MangaDex.Companion.getCoverQualityPrefKey(dexLang: String): String = "${coverQualityPref}_$dexLang"

internal fun MangaDex.Companion.getTryUsingFirstVolumeCoverKey(
    dexLang: String,
): String = "${tryUsingFirstVolumeCoverPref}_$dexLang"

internal fun MangaDex.Companion.getAltTitlesInDescKey(dexLang: String): String = "${altTitlesInDescPref}_$dexLang"

internal fun MangaDex.Companion.getFinalChapterInDescPrefKey(
    dexLang: String,
): String = "${finalChapterInDescPref}_$dexLang"

internal fun MangaDex.Companion.preferExtensionLangTitleKey(
    dexLang: String,
): String = "${preferExtensionLangTitlePref}_$dexLang"
