package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.ui.reader.setting.ReaderOrientation
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.util.system.hasDisplayCutout
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.api.get

private const val AUTOMATIC_BACKGROUND = 3

@Composable
internal fun getDisplayGroup(readerPreferences: ReaderPreferences): Preference.PreferenceGroup {
    val fullscreen by readerPreferences.fullscreen.collectAsState()
    return Preference.PreferenceGroup(
        title = stringResource(MR.strings.pref_category_display),
        preferenceItems = listOf(
            Preference.PreferenceItem.ListPreference(
                preference = readerPreferences.defaultOrientationType,
                entries = ReaderOrientation.entries.drop(1)
                    .associate { it.flagValue to stringResource(it.stringRes) },
                title = stringResource(MR.strings.pref_rotation_type),
            ),
            Preference.PreferenceItem.ListPreference(
                preference = readerPreferences.readerTheme,
                entries = mapOf(
                    1 to stringResource(MR.strings.black_background),
                    2 to stringResource(MR.strings.gray_background),
                    0 to stringResource(MR.strings.white_background),
                    AUTOMATIC_BACKGROUND to stringResource(MR.strings.automatic_background),
                ),
                title = stringResource(MR.strings.pref_reader_theme),
            ),
            Preference.PreferenceItem.SwitchPreference(
                preference = readerPreferences.fullscreen,
                title = stringResource(MR.strings.pref_fullscreen),
            ),
            Preference.PreferenceItem.SwitchPreference(
                preference = readerPreferences.drawUnderCutout,
                title = stringResource(MR.strings.pref_cutout_short),
                enabled = LocalView.current.hasDisplayCutout() && fullscreen,
            ),
            Preference.PreferenceItem.SwitchPreference(
                preference = readerPreferences.keepScreenOn,
                title = stringResource(MR.strings.pref_keep_screen_on),
            ),
            Preference.PreferenceItem.SwitchPreference(
                preference = readerPreferences.showPageNumber,
                title = stringResource(MR.strings.pref_show_page_number),
            ),
        ),
    )
}

@Composable
internal fun getEInkGroup(readerPreferences: ReaderPreferences): Preference.PreferenceGroup {
    val flashPageState by readerPreferences.flashOnPageChange.collectAsState()

    val flashMillisPref = readerPreferences.flashDurationMillis
    val flashMillis by flashMillisPref.collectAsState()

    val flashIntervalPref = readerPreferences.flashPageInterval
    val flashInterval by flashIntervalPref.collectAsState()

    val flashColorPref = readerPreferences.flashColor

    return Preference.PreferenceGroup(
        title = "E-Ink",
        preferenceItems = listOf(
            Preference.PreferenceItem.SwitchPreference(
                preference = readerPreferences.flashOnPageChange,
                title = stringResource(MR.strings.pref_flash_page),
                subtitle = stringResource(MR.strings.pref_flash_page_summ),
            ),
            Preference.PreferenceItem.SliderPreference(
                value = flashMillis / ReaderPreferences.MILLI_CONVERSION,
                valueRange = 1..15,
                title = stringResource(MR.strings.pref_flash_duration),
                valueString = stringResource(MR.strings.pref_flash_duration_summary, flashMillis),
                enabled = flashPageState,
                onValueChanged = { flashMillisPref.set(it * ReaderPreferences.MILLI_CONVERSION) },
            ),
            Preference.PreferenceItem.SliderPreference(
                value = flashInterval,
                valueRange = 1..10,
                title = stringResource(MR.strings.pref_flash_page_interval),
                valueString = pluralStringResource(MR.plurals.pref_pages, flashInterval, flashInterval),
                enabled = flashPageState,
                onValueChanged = { flashIntervalPref.set(it) },
            ),
            Preference.PreferenceItem.ListPreference(
                preference = flashColorPref,
                entries = mapOf(
                    ReaderPreferences.FlashColor.BLACK to stringResource(MR.strings.pref_flash_style_black),
                    ReaderPreferences.FlashColor.WHITE to stringResource(MR.strings.pref_flash_style_white),
                    ReaderPreferences.FlashColor.WHITE_BLACK
                        to stringResource(MR.strings.pref_flash_style_white_black),
                ),
                title = stringResource(MR.strings.pref_flash_with),
                enabled = flashPageState,
            ),
        ),
    )
}

@Composable
internal fun getReadingGroup(readerPreferences: ReaderPreferences): Preference.PreferenceGroup {
    return Preference.PreferenceGroup(
        title = stringResource(MR.strings.pref_category_reading),
        preferenceItems = listOf(
            Preference.PreferenceItem.SwitchPreference(
                preference = readerPreferences.skipRead,
                title = stringResource(MR.strings.pref_skip_read_chapters),
            ),
            Preference.PreferenceItem.SwitchPreference(
                preference = readerPreferences.skipFiltered,
                title = stringResource(MR.strings.pref_skip_filtered_chapters),
            ),
            Preference.PreferenceItem.SwitchPreference(
                preference = readerPreferences.skipDupe,
                title = stringResource(MR.strings.pref_skip_dupe_chapters),
            ),
            Preference.PreferenceItem.SwitchPreference(
                preference = readerPreferences.alwaysShowChapterTransition,
                title = stringResource(MR.strings.pref_always_show_chapter_transition),
            ),
        ),
    )
}
