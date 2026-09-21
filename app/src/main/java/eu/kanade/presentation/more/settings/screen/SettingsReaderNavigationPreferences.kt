package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.ui.reader.setting.ReaderBottomButton
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import eu.kanade.tachiyomi.ui.reader.viewer.pager.PagerConfig
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.api.get

private const val PRELOAD_4_PAGES = 4
private const val PRELOAD_6_PAGES = 6
private const val PRELOAD_8_PAGES = 8
private const val PRELOAD_10_PAGES = 10
private const val PRELOAD_12_PAGES = 12
private const val PRELOAD_14_PAGES = 14
private const val PRELOAD_16_PAGES = 16
private const val PRELOAD_20_PAGES = 20
private const val READER_THREAD_CHOICES = 5

@Composable
internal fun getNavigationGroup(readerPreferences: ReaderPreferences): Preference.PreferenceGroup {
    val readWithVolumeKeysPref = readerPreferences.readWithVolumeKeys
    val readWithVolumeKeys by readWithVolumeKeysPref.collectAsState()

    val verticalNavigator by readerPreferences.verticalNavigator.collectAsState()
    val verticalNavigatorHeightPref = readerPreferences.verticalNavigatorHeight
    val verticalNavigatorHeight by verticalNavigatorHeightPref.collectAsState()

    return Preference.PreferenceGroup(
        title = stringResource(MR.strings.pref_reader_navigation),
        preferenceItems = listOf(
            Preference.PreferenceItem.SwitchPreference(
                preference = readWithVolumeKeysPref,
                title = stringResource(MR.strings.pref_read_with_volume_keys),
            ),
            Preference.PreferenceItem.SwitchPreference(
                preference = readerPreferences.readWithVolumeKeysInverted,
                title = stringResource(MR.strings.pref_read_with_volume_keys_inverted),
                enabled = readWithVolumeKeys,
            ),
            Preference.PreferenceItem.MultiSelectListPreference(
                preference = readerPreferences.verticalNavigator,
                entries = ReadingMode.entries.filter { it != ReadingMode.DEFAULT }
                    .associate { it to stringResource(it.stringRes) },
                title = stringResource(MR.strings.pref_vertical_navigator),
            ),
            Preference.PreferenceItem.SwitchPreference(
                preference = readerPreferences.verticalNavigatorOnLeft,
                title = stringResource(MR.strings.pref_webtoon_vertical_navigator_on_left),
                enabled = verticalNavigator.isNotEmpty(),
            ),
            Preference.PreferenceItem.SliderPreference(
                value = verticalNavigatorHeight,
                valueRange = 65..100,
                steps = 6,
                title = stringResource(MR.strings.pref_vertical_navigator_height),
                onValueChanged = { verticalNavigatorHeightPref.set(it) },
                enabled = verticalNavigator.isNotEmpty(),
            ),
        ),
    )
}

@Composable
internal fun getActionsGroup(readerPreferences: ReaderPreferences): Preference.PreferenceGroup {
    return Preference.PreferenceGroup(
        title = stringResource(MR.strings.pref_reader_actions),
        preferenceItems = listOf(
            Preference.PreferenceItem.SwitchPreference(
                preference = readerPreferences.readWithLongTap,
                title = stringResource(MR.strings.pref_read_with_long_tap),
            ),
            Preference.PreferenceItem.SwitchPreference(
                preference = readerPreferences.folderPerManga,
                title = stringResource(MR.strings.pref_create_folder_per_manga),
                subtitle = stringResource(MR.strings.pref_create_folder_per_manga_summary),
            ),
        ),
    )
}

// SY -->
@Composable
internal fun getPageDownloadingGroup(readerPreferences: ReaderPreferences): Preference.PreferenceGroup {
    return Preference.PreferenceGroup(
        title = stringResource(SYMR.strings.page_downloading),
        preferenceItems = listOf(
            Preference.PreferenceItem.ListPreference(
                preference = readerPreferences.preloadSize,
                title = stringResource(SYMR.strings.reader_preload_amount),
                subtitle = stringResource(SYMR.strings.reader_preload_amount_summary),
                entries = mapOf(
                    PRELOAD_4_PAGES to stringResource(SYMR.strings.reader_preload_amount_4_pages),
                    PRELOAD_6_PAGES to stringResource(SYMR.strings.reader_preload_amount_6_pages),
                    PRELOAD_8_PAGES to stringResource(SYMR.strings.reader_preload_amount_8_pages),
                    PRELOAD_10_PAGES to stringResource(SYMR.strings.reader_preload_amount_10_pages),
                    PRELOAD_12_PAGES to stringResource(SYMR.strings.reader_preload_amount_12_pages),
                    PRELOAD_14_PAGES to stringResource(SYMR.strings.reader_preload_amount_14_pages),
                    PRELOAD_16_PAGES to stringResource(SYMR.strings.reader_preload_amount_16_pages),
                    PRELOAD_20_PAGES to stringResource(SYMR.strings.reader_preload_amount_20_pages),
                ),
            ),
            Preference.PreferenceItem.ListPreference(
                preference = readerPreferences.readerThreads,
                title = stringResource(SYMR.strings.download_threads),
                subtitle = stringResource(SYMR.strings.download_threads_summary),
                entries = List(READER_THREAD_CHOICES) { it }.associateWith { it.toString() },
            ),
            Preference.PreferenceItem.ListPreference(
                preference = readerPreferences.cacheSize,
                title = stringResource(SYMR.strings.reader_cache_size),
                subtitle = stringResource(SYMR.strings.reader_cache_size_summary),
                entries = mapOf(
                    "50" to "50 MB",
                    "75" to "75 MB",
                    "100" to "100 MB",
                    "150" to "150 MB",
                    "250" to "250 MB",
                    "500" to "500 MB",
                    "750" to "750 MB",
                    "1000" to "1 GB",
                    "1500" to "1.5 GB",
                    "2000" to "2 GB",
                    "2500" to "2.5 GB",
                    "3000" to "3 GB",
                    "3500" to "3.5 GB",
                    "4000" to "4 GB",
                    "4500" to "4.5 GB",
                    "5000" to "5 GB",
                ),
            ),
            Preference.PreferenceItem.SwitchPreference(
                preference = readerPreferences.aggressivePageLoading,
                title = stringResource(SYMR.strings.aggressively_load_pages),
                subtitle = stringResource(SYMR.strings.aggressively_load_pages_summary),
            ),
        ),
    )
}

@Composable
internal fun getForkSettingsGroup(readerPreferences: ReaderPreferences): Preference.PreferenceGroup {
    val pageLayout by readerPreferences.pageLayout.collectAsState()
    return Preference.PreferenceGroup(
        title = stringResource(SYMR.strings.pref_category_fork),
        preferenceItems = listOf(
            Preference.PreferenceItem.SwitchPreference(
                preference = readerPreferences.readerInstantRetry,
                title = stringResource(SYMR.strings.skip_queue_on_retry),
                subtitle = stringResource(SYMR.strings.skip_queue_on_retry_summary),
            ),
            Preference.PreferenceItem.SwitchPreference(
                preference = readerPreferences.preserveReadingPosition,
                title = stringResource(SYMR.strings.preserve_reading_position),
            ),
            Preference.PreferenceItem.SwitchPreference(
                preference = readerPreferences.useAutoWebtoon,
                title = stringResource(SYMR.strings.auto_webtoon_mode),
                subtitle = stringResource(SYMR.strings.auto_webtoon_mode_summary),
            ),
            Preference.PreferenceItem.MultiSelectListPreference(
                preference = readerPreferences.readerBottomButtons,
                title = stringResource(SYMR.strings.reader_bottom_buttons),
                subtitle = stringResource(SYMR.strings.reader_bottom_buttons_summary),
                entries = ReaderBottomButton.entries
                    .associate { it.value to stringResource(it.stringRes) },
            ),
            Preference.PreferenceItem.ListPreference(
                preference = readerPreferences.pageLayout,
                title = stringResource(SYMR.strings.page_layout),
                subtitle = stringResource(SYMR.strings.automatic_can_still_switch),
                entries = ReaderPreferences.PageLayouts
                    .mapIndexed { index, titleRes -> index to stringResource(titleRes) }
                    .toMap(),
            ),
            Preference.PreferenceItem.SwitchPreference(
                preference = readerPreferences.invertDoublePages,
                title = stringResource(SYMR.strings.invert_double_pages),
                enabled = pageLayout != PagerConfig.PageLayout.SINGLE_PAGE,
            ),
            Preference.PreferenceItem.ListPreference(
                preference = readerPreferences.centerMarginType,
                title = stringResource(SYMR.strings.center_margin),
                subtitle = stringResource(SYMR.strings.pref_center_margin_summary),
                entries = ReaderPreferences.CenterMarginTypes
                    .mapIndexed { index, titleRes -> index + 1 to stringResource(titleRes) }
                    .toMap(),
            ),
            Preference.PreferenceItem.ListPreference(
                preference = readerPreferences.archiveReaderMode,
                title = stringResource(SYMR.strings.pref_archive_reader_mode),
                subtitle = stringResource(SYMR.strings.pref_archive_reader_mode_summary),
                entries = ReaderPreferences.archiveModeTypes
                    .mapIndexed { index, titleRes -> index to stringResource(titleRes) }
                    .toMap(),
            ),
        ),
    )
}
