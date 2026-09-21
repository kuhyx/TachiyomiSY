package eu.kanade.presentation.reader.settings

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.setting.dualPageRotateToFit
import tachiyomi.core.common.preference.Preference
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.CheckboxItem
import tachiyomi.presentation.core.components.SettingsChipRow
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

// The navigation-mode preference value that turns tap zones off.
private const val NAVIGATION_DISABLED = 5

// A chip per entry; the preference stores the entry's index plus `offset`.
@Composable
internal fun IndexedChipRow(
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
internal fun DualPageItems(
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

@Composable
internal fun ColumnScope.TapZonesItems(
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
