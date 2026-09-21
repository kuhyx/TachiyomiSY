package eu.kanade.presentation.reader.settings

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import dev.icerock.moko.resources.StringResource
import eu.kanade.domain.manga.model.readerOrientation
import eu.kanade.domain.manga.model.readingMode
import eu.kanade.tachiyomi.ui.reader.setting.ReaderOrientation
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.setting.ReaderSettingsScreenModel
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonViewer
import tachiyomi.core.common.preference.Preference
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.components.CheckboxItem
import tachiyomi.presentation.core.components.HeadingItem
import tachiyomi.presentation.core.components.SettingsChipRow
import tachiyomi.presentation.core.components.SliderItem
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import java.text.NumberFormat

private const val PERCENT = 100f

// The navigation-mode preference value that turns tap zones off.
private const val NAVIGATION_DISABLED = 5

@Composable
internal fun ColumnScope.ReadingModePage(screenModel: ReaderSettingsScreenModel) {
    HeadingItem(MR.strings.pref_category_for_this_series)
    val manga by screenModel.mangaFlow.collectAsState()

    val readingMode = remember(manga) { ReadingMode.fromPreference(manga?.readingMode?.toInt()) }
    SettingsChipRow(MR.strings.pref_category_reading_mode) {
        ReadingMode.entries.map {
            FilterChip(
                selected = it == readingMode,
                onClick = { screenModel.onChangeReadingMode(it) },
                label = { Text(stringResource(it.stringRes)) },
            )
        }
    }

    val orientation = remember(manga) { ReaderOrientation.fromPreference(manga?.readerOrientation?.toInt()) }
    SettingsChipRow(MR.strings.rotation_type) {
        ReaderOrientation.entries.map {
            FilterChip(
                selected = it == orientation,
                onClick = { screenModel.onChangeOrientation(it) },
                label = { Text(stringResource(it.stringRes)) },
            )
        }
    }

    val viewer by screenModel.viewerFlow.collectAsState()
    if (viewer is WebtoonViewer) {
        WebtoonViewerSettings(screenModel)
        // SY -->
        WebtoonWithGapsViewerSettings(screenModel)
        // SY <--
    } else {
        PagerViewerSettings(screenModel)
    }
}

@Composable
private fun ColumnScope.PagerViewerSettings(screenModel: ReaderSettingsScreenModel) {
    HeadingItem(MR.strings.pager_viewer)

    val navigationModePager by screenModel.preferences.navigationModePager.collectAsState()
    val pagerNavInverted by screenModel.preferences.pagerNavInverted.collectAsState()
    TapZonesItems(
        selected = navigationModePager,
        onSelect = screenModel.preferences.navigationModePager::set,
        invertMode = pagerNavInverted,
        onSelectInvertMode = screenModel.preferences.pagerNavInverted::set,
    )

    // Scale type and zoom start are stored 1-based; page layout and centre margin 0-based.
    IndexedChipRow(
        MR.strings.pref_image_scale_type,
        ReaderPreferences.ImageScaleType,
        screenModel.preferences.imageScaleType,
        1,
    )
    IndexedChipRow(MR.strings.pref_zoom_start, ReaderPreferences.ZoomStart, screenModel.preferences.zoomStart, 1)
    // SY -->
    IndexedChipRow(SYMR.strings.page_layout, ReaderPreferences.PageLayouts, screenModel.preferences.pageLayout, 0)
    // SY <--

    CheckboxItem(
        label = stringResource(MR.strings.pref_crop_borders),
        pref = screenModel.preferences.cropBorders,
    )

    CheckboxItem(
        label = stringResource(MR.strings.pref_landscape_zoom),
        pref = screenModel.preferences.landscapeZoom,
    )

    CheckboxItem(
        label = stringResource(MR.strings.pref_navigate_pan),
        pref = screenModel.preferences.navigateToPan,
    )

    DualPageItems(
        split = screenModel.preferences.dualPageSplitPaged,
        invert = screenModel.preferences.dualPageInvertPaged,
        rotateToFit = screenModel.preferences.dualPageRotateToFit,
        rotateToFitInvert = screenModel.preferences.dualPageRotateToFitInvert,
    )

    // SY -->
    CheckboxItem(
        label = stringResource(MR.strings.pref_page_transitions),
        pref = screenModel.preferences.pageTransitionsPager,
    )

    CheckboxItem(
        label = stringResource(SYMR.strings.invert_double_pages),
        pref = screenModel.preferences.invertDoublePages,
    )

    IndexedChipRow(
        SYMR.strings.pref_center_margin,
        ReaderPreferences.CenterMarginTypes,
        screenModel.preferences.centerMarginType,
        0,
    )
    // SY <--
}

@Composable
private fun ColumnScope.WebtoonViewerSettings(screenModel: ReaderSettingsScreenModel) {
    val numberFormat = remember { NumberFormat.getPercentInstance() }

    HeadingItem(MR.strings.webtoon_viewer)

    val navigationModeWebtoon by screenModel.preferences.navigationModeWebtoon.collectAsState()
    val webtoonNavInverted by screenModel.preferences.webtoonNavInverted.collectAsState()
    TapZonesItems(
        selected = navigationModeWebtoon,
        onSelect = screenModel.preferences.navigationModeWebtoon::set,
        invertMode = webtoonNavInverted,
        onSelectInvertMode = screenModel.preferences.webtoonNavInverted::set,
    )

    val webtoonSidePadding by screenModel.preferences.webtoonSidePadding.collectAsState()
    SliderItem(
        value = webtoonSidePadding,
        valueRange = ReaderPreferences.let { it.WEBTOON_PADDING_MIN..it.WEBTOON_PADDING_MAX },
        label = stringResource(MR.strings.pref_webtoon_side_padding),
        valueString = numberFormat.format(webtoonSidePadding / PERCENT),
        onChange = {
            screenModel.preferences.webtoonSidePadding.set(it)
        },
        pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    )

    CheckboxItem(
        label = stringResource(MR.strings.pref_crop_borders),
        pref = screenModel.preferences.cropBordersWebtoon,
    )

    // SY -->
    CheckboxItem(
        label = stringResource(SYMR.strings.pref_smooth_scroll),
        pref = screenModel.preferences.smoothAutoScroll,
    )

    CheckboxItem(
        label = stringResource(MR.strings.pref_page_transitions),
        pref = screenModel.preferences.pageTransitionsWebtoon,
    )
    // SY <--

    DualPageItems(
        split = screenModel.preferences.dualPageSplitWebtoon,
        invert = screenModel.preferences.dualPageInvertWebtoon,
        rotateToFit = screenModel.preferences.dualPageRotateToFitWebtoon,
        rotateToFitInvert = screenModel.preferences.dualPageRotateToFitInvertWebtoon,
    )

    CheckboxItem(
        label = stringResource(MR.strings.pref_double_tap_zoom),
        pref = screenModel.preferences.webtoonDoubleTapZoomEnabled,
    )
    CheckboxItem(
        label = stringResource(MR.strings.pref_webtoon_disable_zoom_out),
        pref = screenModel.preferences.webtoonDisableZoomOut,
    )
}

// A chip per entry; the preference stores the entry's index plus `offset`.
@Composable
private fun IndexedChipRow(
    labelRes: StringResource,
    entries: List<StringResource>,
    pref: Preference<Int>,
    offset: Int,
) {
    val selected by pref.collectAsState()
    SettingsChipRow(labelRes) {
        entries.mapIndexed { index, titleRes ->
            FilterChip(
                selected = selected == index + offset,
                onClick = { pref.set(index + offset) },
                label = { Text(stringResource(titleRes)) },
            )
        }
    }
}

// Dual-page split and rotate-to-fit, each revealing its "invert" option once enabled.
@Composable
private fun DualPageItems(
    split: Preference<Boolean>,
    invert: Preference<Boolean>,
    rotateToFit: Preference<Boolean>,
    rotateToFitInvert: Preference<Boolean>,
) {
    val dualPageSplit by split.collectAsState()
    CheckboxItem(
        label = stringResource(MR.strings.pref_dual_page_split),
        pref = split,
    )

    if (dualPageSplit) {
        CheckboxItem(
            label = stringResource(MR.strings.pref_dual_page_invert),
            pref = invert,
        )
    }

    val dualPageRotateToFit by rotateToFit.collectAsState()
    CheckboxItem(
        label = stringResource(MR.strings.pref_page_rotate),
        pref = rotateToFit,
    )

    if (dualPageRotateToFit) {
        CheckboxItem(
            label = stringResource(MR.strings.pref_page_rotate_invert),
            pref = rotateToFitInvert,
        )
    }
}

// SY -->
@Composable
private fun ColumnScope.WebtoonWithGapsViewerSettings(screenModel: ReaderSettingsScreenModel) {
    HeadingItem(MR.strings.vertical_plus_viewer)

    CheckboxItem(
        label = stringResource(MR.strings.pref_crop_borders),
        pref = screenModel.preferences.cropBordersContinuousVertical,
    )
}
// SY <--

@Composable
private fun ColumnScope.TapZonesItems(
    selected: Int,
    onSelect: (Int) -> Unit,
    invertMode: ReaderPreferences.TappingInvertMode,
    onSelectInvertMode: (ReaderPreferences.TappingInvertMode) -> Unit,
) {
    SettingsChipRow(MR.strings.pref_viewer_nav) {
        ReaderPreferences.TapZones.mapIndexed { index, titleRes ->
            FilterChip(
                selected = selected == index,
                onClick = { onSelect(index) },
                label = { Text(stringResource(titleRes)) },
            )
        }
    }

    if (selected != NAVIGATION_DISABLED) {
        SettingsChipRow(MR.strings.pref_read_with_tapping_inverted) {
            ReaderPreferences.TappingInvertMode.entries.map {
                FilterChip(
                    selected = it == invertMode,
                    onClick = { onSelectInvertMode(it) },
                    label = { Text(stringResource(it.titleRes)) },
                )
            }
        }
    }
}
