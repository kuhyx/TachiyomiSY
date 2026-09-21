package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import tachiyomi.core.common.preference.Preference as CorePreference

// The preference rows the pager and webtoon viewer groups both carry.

// Tap-zone layout plus its inversion; inversion is moot for the "disabled" layout (index 5).
@Composable
internal fun tapZonePreferences(
    navModePref: CorePreference<Int>,
    invertedPref: CorePreference<ReaderPreferences.TappingInvertMode>,
): List<Preference.PreferenceItem<out Any, out Any>> {
    val navMode by navModePref.collectAsState()
    return listOf(
        Preference.PreferenceItem.ListPreference(
            preference = navModePref,
            entries = ReaderPreferences.TapZones
                .mapIndexed { index, titleRes -> index to stringResource(titleRes) }
                .toMap(),
            title = stringResource(MR.strings.pref_viewer_nav),
        ),
        Preference.PreferenceItem.ListPreference(
            preference = invertedPref,
            entries = listOf(
                ReaderPreferences.TappingInvertMode.NONE,
                ReaderPreferences.TappingInvertMode.HORIZONTAL,
                ReaderPreferences.TappingInvertMode.VERTICAL,
                ReaderPreferences.TappingInvertMode.BOTH,
            )
                .associateWith { stringResource(it.titleRes) },
            title = stringResource(MR.strings.pref_read_with_tapping_inverted),
            enabled = navMode != 5,
        ),
    )
}

// Split and rotate-to-fit are mutually exclusive: enabling one clears the other.
@Composable
internal fun dualPagePreferences(
    splitPref: CorePreference<Boolean>,
    invertPref: CorePreference<Boolean>,
    rotatePref: CorePreference<Boolean>,
    rotateInvertPref: CorePreference<Boolean>,
): List<Preference.PreferenceItem<out Any, out Any>> {
    val dualPageSplit by splitPref.collectAsState()
    val rotateToFit by rotatePref.collectAsState()
    return listOf(
        Preference.PreferenceItem.SwitchPreference(
            preference = splitPref,
            title = stringResource(MR.strings.pref_dual_page_split),
            onValueChanged = {
                rotatePref.set(false)
                true
            },
        ),
        Preference.PreferenceItem.SwitchPreference(
            preference = invertPref,
            title = stringResource(MR.strings.pref_dual_page_invert),
            subtitle = stringResource(MR.strings.pref_dual_page_invert_summary),
            enabled = dualPageSplit,
        ),
        Preference.PreferenceItem.SwitchPreference(
            preference = rotatePref,
            title = stringResource(MR.strings.pref_page_rotate),
            onValueChanged = {
                splitPref.set(false)
                true
            },
        ),
        Preference.PreferenceItem.SwitchPreference(
            preference = rotateInvertPref,
            title = stringResource(MR.strings.pref_page_rotate_invert),
            enabled = rotateToFit,
        ),
    )
}
