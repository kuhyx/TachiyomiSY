package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.api.get
import java.text.NumberFormat

private const val PERCENT = 100f

@Composable
internal fun getPagedGroup(readerPreferences: ReaderPreferences): Preference.PreferenceGroup {
    val navMode by readerPreferences.navigationModePager.collectAsState()
    val imageScaleType by readerPreferences.imageScaleType.collectAsState()
    val pagedItems = listOf(
        Preference.PreferenceItem.ListPreference(
            preference = readerPreferences.imageScaleType,
            entries = ReaderPreferences.ImageScaleType
                .mapIndexed { index, titleRes -> index + 1 to stringResource(titleRes) }
                .toMap(),
            title = stringResource(MR.strings.pref_image_scale_type),
        ),
        Preference.PreferenceItem.ListPreference(
            preference = readerPreferences.zoomStart,
            entries = ReaderPreferences.ZoomStart
                .mapIndexed { index, titleRes -> index + 1 to stringResource(titleRes) }
                .toMap(),
            title = stringResource(MR.strings.pref_zoom_start),
        ),
        Preference.PreferenceItem.SwitchPreference(
            preference = readerPreferences.cropBorders,
            title = stringResource(MR.strings.pref_crop_borders),
        ),
        // SY -->
        Preference.PreferenceItem.SwitchPreference(
            preference = readerPreferences.pageTransitionsPager,
            title = stringResource(MR.strings.pref_page_transitions),
        ),
        // SY <--
        Preference.PreferenceItem.SwitchPreference(
            preference = readerPreferences.landscapeZoom,
            title = stringResource(MR.strings.pref_landscape_zoom),
            enabled = imageScaleType == 1,
        ),
        Preference.PreferenceItem.SwitchPreference(
            preference = readerPreferences.navigateToPan,
            title = stringResource(MR.strings.pref_navigate_pan),
            enabled = navMode != 5,
        ),
    )
    return Preference.PreferenceGroup(
        title = stringResource(MR.strings.pager_viewer),
        preferenceItems =
        tapZonePreferences(readerPreferences.navigationModePager, readerPreferences.pagerNavInverted) +
            pagedItems +
            dualPagePreferences(
                splitPref = readerPreferences.dualPageSplitPaged,
                invertPref = readerPreferences.dualPageInvertPaged,
                rotatePref = readerPreferences.dualPageRotateToFit,
                rotateInvertPref = readerPreferences.dualPageRotateToFitInvert,
            ),
    )
}

@Composable
internal fun getWebtoonGroup(readerPreferences: ReaderPreferences): Preference.PreferenceGroup {
    val numberFormat = remember { NumberFormat.getPercentInstance() }
    val webtoonSidePaddingPref = readerPreferences.webtoonSidePadding
    val webtoonSidePadding by webtoonSidePaddingPref.collectAsState()
    val webtoonItems = listOf(
        Preference.PreferenceItem.SliderPreference(
            value = webtoonSidePadding,
            valueRange = ReaderPreferences.let {
                it.WEBTOON_PADDING_MIN..it.WEBTOON_PADDING_MAX
            },
            title = stringResource(MR.strings.pref_webtoon_side_padding),
            valueString = numberFormat.format(webtoonSidePadding / PERCENT),
            onValueChanged = { webtoonSidePaddingPref.set(it) },
        ),
        Preference.PreferenceItem.ListPreference(
            preference = readerPreferences.readerHideThreshold,
            entries = mapOf(
                ReaderPreferences.ReaderHideThreshold.HIGHEST to stringResource(MR.strings.pref_highest),
                ReaderPreferences.ReaderHideThreshold.HIGH to stringResource(MR.strings.pref_high),
                ReaderPreferences.ReaderHideThreshold.LOW to stringResource(MR.strings.pref_low),
                ReaderPreferences.ReaderHideThreshold.LOWEST to stringResource(MR.strings.pref_lowest),
            ),
            title = stringResource(MR.strings.pref_hide_threshold),
        ),
        Preference.PreferenceItem.SwitchPreference(
            preference = readerPreferences.cropBordersWebtoon,
            title = stringResource(MR.strings.pref_crop_borders),
        ),
    )
    val zoomItems = listOf(
        Preference.PreferenceItem.SwitchPreference(
            preference = readerPreferences.webtoonDoubleTapZoomEnabled,
            title = stringResource(MR.strings.pref_double_tap_zoom),
        ),
        Preference.PreferenceItem.SwitchPreference(
            preference = readerPreferences.webtoonDisableZoomOut,
            title = stringResource(MR.strings.pref_webtoon_disable_zoom_out),
        ),
        // SY -->
        Preference.PreferenceItem.SwitchPreference(
            preference = readerPreferences.pageTransitionsWebtoon,
            title = stringResource(MR.strings.pref_page_transitions),
        ),
        // SY <--
    )
    return Preference.PreferenceGroup(
        title = stringResource(MR.strings.webtoon_viewer),
        preferenceItems =
        tapZonePreferences(readerPreferences.navigationModeWebtoon, readerPreferences.webtoonNavInverted) +
            webtoonItems +
            dualPagePreferences(
                splitPref = readerPreferences.dualPageSplitWebtoon,
                invertPref = readerPreferences.dualPageInvertWebtoon,
                rotatePref = readerPreferences.dualPageRotateToFitWebtoon,
                rotateInvertPref = readerPreferences.dualPageRotateToFitInvertWebtoon,
            ) +
            zoomItems,
    )
}

// SY -->
@Composable
internal fun getContinuousVerticalGroup(readerPreferences: ReaderPreferences): Preference.PreferenceGroup {
    return Preference.PreferenceGroup(
        title = stringResource(MR.strings.vertical_plus_viewer),
        preferenceItems = listOf(
            Preference.PreferenceItem.SwitchPreference(
                preference = readerPreferences.continuousVerticalTappingByPage,
                title = stringResource(SYMR.strings.tap_scroll_page),
                subtitle = stringResource(SYMR.strings.tap_scroll_page_summary),
            ),
            Preference.PreferenceItem.SwitchPreference(
                preference = readerPreferences.cropBordersContinuousVertical,
                title = stringResource(MR.strings.pref_crop_borders),
            ),
        ),
    )
}
