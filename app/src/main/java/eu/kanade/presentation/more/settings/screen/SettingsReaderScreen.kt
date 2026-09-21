package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

// Preference values the reader settings offer.
private const val DOUBLE_TAP_NORMAL_MS = 500
private const val DOUBLE_TAP_FAST_MS = 250

internal object SettingsReaderScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_category_reader

    @Composable
    override fun getPreferences(): List<Preference> {
        val readerPref = remember { Injekt.get<ReaderPreferences>() }

        return listOf(
            Preference.PreferenceItem.ListPreference(
                preference = readerPref.defaultReadingMode,
                entries = ReadingMode.entries.drop(1)
                    .associate { it.flagValue to stringResource(it.stringRes) },
                title = stringResource(MR.strings.pref_viewer_type),
            ),
            Preference.PreferenceItem.ListPreference(
                preference = readerPref.doubleTapAnimSpeed,
                entries = mapOf(
                    1 to stringResource(MR.strings.double_tap_anim_speed_0),
                    DOUBLE_TAP_NORMAL_MS to stringResource(MR.strings.double_tap_anim_speed_normal),
                    DOUBLE_TAP_FAST_MS to stringResource(MR.strings.double_tap_anim_speed_fast),
                ),
                title = stringResource(MR.strings.pref_double_tap_anim_speed),
            ),
            Preference.PreferenceItem.SwitchPreference(
                preference = readerPref.showReadingMode,
                title = stringResource(MR.strings.pref_show_reading_mode),
                subtitle = stringResource(MR.strings.pref_show_reading_mode_summary),
            ),
            Preference.PreferenceItem.SwitchPreference(
                preference = readerPref.showNavigationOverlayOnStart,
                title = stringResource(MR.strings.pref_show_navigation_mode),
                subtitle = stringResource(MR.strings.pref_show_navigation_mode_summary),
            ),
            /* SY -->
            Preference.PreferenceItem.SwitchPreference(
                preference = readerPref.pageTransitions,
                title = stringResource(MR.strings.pref_page_transitions),
            ),
            SY <-- */
            getDisplayGroup(readerPreferences = readerPref),
            getEInkGroup(readerPreferences = readerPref),
            getReadingGroup(readerPreferences = readerPref),
            getPagedGroup(readerPreferences = readerPref),
            getWebtoonGroup(readerPreferences = readerPref),
            // SY -->
            getContinuousVerticalGroup(readerPreferences = readerPref),
            // SY <--
            getNavigationGroup(readerPreferences = readerPref),
            getActionsGroup(readerPreferences = readerPref),
            // SY -->
            getPageDownloadingGroup(readerPreferences = readerPref),
            getForkSettingsGroup(readerPreferences = readerPref),
            // SY <--
        )
    }

    // SY <--

    // SY <--
}
