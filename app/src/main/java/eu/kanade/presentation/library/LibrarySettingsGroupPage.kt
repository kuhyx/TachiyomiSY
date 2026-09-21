package eu.kanade.presentation.library

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.util.fastForEach
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.library.LibrarySettingsScreenModel
import kotlinx.coroutines.flow.map
import tachiyomi.domain.library.model.LibraryGroup
import tachiyomi.presentation.core.components.IconItem
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

internal fun groupTypeDrawableRes(type: Int): Int {
    return when (type) {
        LibraryGroup.BY_STATUS -> R.drawable.ic_progress_clock_24dp
        LibraryGroup.BY_TRACK_STATUS -> R.drawable.ic_sync_24dp
        LibraryGroup.BY_SOURCE -> R.drawable.ic_browse_filled_24dp
        LibraryGroup.UNGROUPED -> R.drawable.ic_ungroup_24dp
        else -> R.drawable.ic_label_24dp
    }
}

@Composable
internal fun ColumnScope.GroupPage(
    screenModel: LibrarySettingsScreenModel,
    hasCategories: Boolean,
) {
    val trackers by screenModel.trackersFlow.collectAsState()
    val groups = remember(hasCategories, trackers) {
        buildList {
            add(LibraryGroup.BY_DEFAULT)
            add(LibraryGroup.BY_SOURCE)
            add(LibraryGroup.BY_STATUS)
            if (trackers.isNotEmpty()) {
                add(LibraryGroup.BY_TRACK_STATUS)
            }
            if (hasCategories) {
                add(LibraryGroup.UNGROUPED)
            }
        }.map {
            GroupMode(
                it,
                LibraryGroup.groupTypeStringRes(it, hasCategories),
                groupTypeDrawableRes(it),
            )
        }
    }

    groups.fastForEach {
        IconItem(
            label = stringResource(it.nameRes),
            icon = painterResource(it.drawableRes),
            selected = it.int == screenModel.grouping,
            onClick = {
                screenModel.setGrouping(it.int)
            },
        )
    }
}
