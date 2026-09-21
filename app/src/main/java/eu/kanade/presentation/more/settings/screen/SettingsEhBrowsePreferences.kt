package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import eu.kanade.presentation.more.settings.Preference
import exh.source.ExhPreferences
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource

/*
 * The language, front-page category and browsing preferences of the E-Hentai settings screen.
 * Part of [SettingsEhScreen]; same package, so its getPreferences() calls them as before.
 */

@Composable
internal fun watchedListDefaultState(
    exhentaiEnabled: Boolean,
    exhPreferences: ExhPreferences,
): Preference.PreferenceItem.SwitchPreference {
    return Preference.PreferenceItem.SwitchPreference(
        preference = exhPreferences.exhWatchedListDefaultState,
        title = stringResource(SYMR.strings.watched_list_default),
        subtitle = stringResource(SYMR.strings.watched_list_state_summary),
        enabled = exhentaiEnabled,
    )
}

@Composable
internal fun imageQuality(
    exhentaiEnabled: Boolean,
    exhPreferences: ExhPreferences,
): Preference.PreferenceItem.ListPreference<String> {
    return Preference.PreferenceItem.ListPreference(
        preference = exhPreferences.imageQuality,
        title = stringResource(SYMR.strings.eh_image_quality_summary),
        subtitle = stringResource(SYMR.strings.eh_image_quality),
        entries = mapOf(
            "auto" to stringResource(SYMR.strings.eh_image_quality_auto),
            "ovrs_2400" to stringResource(SYMR.strings.eh_image_quality_2400),
            "ovrs_1600" to stringResource(SYMR.strings.eh_image_quality_1600),
            "high" to stringResource(SYMR.strings.eh_image_quality_1280),
            "med" to stringResource(SYMR.strings.eh_image_quality_980),
            "low" to stringResource(SYMR.strings.eh_image_quality_780),
        ),
        enabled = exhentaiEnabled,
    )
}

@Composable
internal fun enhancedEhentaiView(exhPreferences: ExhPreferences): Preference.PreferenceItem.SwitchPreference {
    return Preference.PreferenceItem.SwitchPreference(
        preference = exhPreferences.enhancedEHentaiView,
        title = stringResource(SYMR.strings.pref_enhanced_e_hentai_view),
        subtitle = stringResource(SYMR.strings.pref_enhanced_e_hentai_view_summary),
    )
}
