package eu.kanade.presentation.library

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.ui.library.LibrarySettingsScreenModel
import eu.kanade.tachiyomi.util.system.isDebugBuildType
import eu.kanade.tachiyomi.util.system.isPreviewBuildType
import kotlinx.coroutines.flow.map
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.components.HeadingItem
import tachiyomi.presentation.core.components.TriStateItem
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import kotlin.reflect.KProperty1

@Composable
internal fun ColumnScope.FilterPage(
    screenModel: LibrarySettingsScreenModel,
) {
    val filterDownloaded by screenModel.libraryPreferences.filterDownloaded.collectAsState()
    val downloadedOnly by screenModel.preferences.downloadedOnly.collectAsState()
    val autoUpdateMangaRestrictions by screenModel.libraryPreferences.autoUpdateMangaRestrictions.collectAsState()

    TriStateItem(
        label = stringResource(MR.strings.label_downloaded),
        state = if (downloadedOnly) {
            TriState.ENABLED_IS
        } else {
            filterDownloaded
        },
        enabled = !downloadedOnly,
        onClick = { screenModel.toggleFilter(LibraryPreferences::filterDownloaded) },
    )
    PreferenceTriStateItem(screenModel, MR.strings.action_filter_unread, LibraryPreferences::filterUnread)
    PreferenceTriStateItem(screenModel, MR.strings.label_started, LibraryPreferences::filterStarted)
    PreferenceTriStateItem(screenModel, MR.strings.action_filter_bookmarked, LibraryPreferences::filterBookmarked)
    PreferenceTriStateItem(screenModel, MR.strings.completed, LibraryPreferences::filterCompleted)
    // Follow-up: re-enable when custom intervals are ready for stable (https://github.com/kuhyx/TachiyomiSY/issues/10)
    if (
        (isDebugBuildType || isPreviewBuildType) &&
        LibraryPreferences.MANGA_OUTSIDE_RELEASE_PERIOD in autoUpdateMangaRestrictions
    ) {
        PreferenceTriStateItem(
            screenModel,
            MR.strings.action_filter_interval_custom,
            LibraryPreferences::filterIntervalCustom,
        )
    }
    // SY -->
    PreferenceTriStateItem(screenModel, SYMR.strings.lewd, LibraryPreferences::filterLewd)
    // SY <--
    TrackerFilters(screenModel)
}

// A tri-state filter row backed directly by one of the library preferences.
@Composable
internal fun PreferenceTriStateItem(
    screenModel: LibrarySettingsScreenModel,
    label: StringResource,
    preference: KProperty1<LibraryPreferences, Preference<TriState>>,
) {
    val state by preference.get(screenModel.libraryPreferences).collectAsState()
    TriStateItem(
        label = stringResource(label),
        state = state,
        onClick = { screenModel.toggleFilter(preference) },
    )
}

// One tracker gets a single "tracked" row; several get a heading with a row each.
@Composable
internal fun ColumnScope.TrackerFilters(screenModel: LibrarySettingsScreenModel) {
    val trackers by screenModel.trackersFlow.collectAsState()
    when (trackers.size) {
        0 -> {
            // No trackers
        }
        1 -> {
            val service = trackers[0]
            val filterTracker by screenModel.libraryPreferences.filterTracking(service.id.toInt()).collectAsState()
            TriStateItem(
                label = stringResource(MR.strings.action_filter_tracked),
                state = filterTracker,
                onClick = { screenModel.toggleTracker(service.id.toInt()) },
            )
        }
        else -> {
            HeadingItem(MR.strings.action_filter_tracked)
            trackers.map { service ->
                val filterTracker by screenModel.libraryPreferences.filterTracking(service.id.toInt()).collectAsState()
                TriStateItem(
                    label = service.name,
                    state = filterTracker,
                    onClick = { screenModel.toggleTracker(service.id.toInt()) },
                )
            }
        }
    }
}
