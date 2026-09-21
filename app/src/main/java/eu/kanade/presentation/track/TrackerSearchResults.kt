package eu.kanade.presentation.track

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.util.plus
import tachiyomi.presentation.core.util.secondaryItemAlpha

// null result = still loading; a failed result shows its message; an empty list shows "no results".
@Composable
internal fun TrackerSearchResults(
    queryResult: Result<List<TrackSearch>>?,
    selected: TrackSearch?,
    onSelectedChange: (TrackSearch) -> Unit,
    innerPadding: PaddingValues,
) {
    if (queryResult == null) {
        LoadingScreen(modifier = Modifier.padding(innerPadding))
        return
    }
    val availableTracks = queryResult.getOrNull()
    when {
        availableTracks == null -> EmptyScreen(
            modifier = Modifier.padding(innerPadding),
            message = queryResult.exceptionOrNull()?.message
                ?: stringResource(MR.strings.unknown_error),
        )
        availableTracks.isEmpty() -> EmptyScreen(
            modifier = Modifier.padding(innerPadding),
            stringRes = MR.strings.no_results_found,
        )
        else -> ScrollbarLazyColumn(
            contentPadding = innerPadding + PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(
                items = availableTracks,
                key = { it.hashCode() },
            ) {
                SearchResultItem(
                    trackSearch = it,
                    selected = it == selected,
                    onClick = { onSelectedChange(it) },
                )
            }
        }
    }
}

@Composable
internal fun SearchResultItem(
    trackSearch: TrackSearch,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val description = trackSearch.summary.trim()
    var dropDownMenuExpanded by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .resultCard(selected)
            .combinedClickable(
                onLongClick = { dropDownMenuExpanded = true },
                onClick = {
                    focusManager.clearFocus()
                    onClick()
                },
            )
            .padding(12.dp),
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                modifier = Modifier.align(Alignment.TopEnd),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Column {
            Row {
                MangaCover.Book(
                    data = trackSearch.coverUrl,
                    modifier = Modifier.height(96.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                SearchResultInfo(
                    trackSearch = trackSearch,
                    menuExpanded = dropDownMenuExpanded,
                    onCollapseMenu = { dropDownMenuExpanded = false },
                )
            }
            if (description.isNotBlank()) {
                Text(
                    text = description,
                    modifier = Modifier
                        .paddingFromBaseline(top = 24.dp)
                        .secondaryItemAlpha(),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

// Rounded surface card; the outline is only drawn for the selected result.
@Composable
internal fun Modifier.resultCard(selected: Boolean): Modifier {
    val shape = RoundedCornerShape(16.dp)
    val borderColor = if (selected) MaterialTheme.colorScheme.outline else Color.Transparent
    return this
        .fillMaxWidth()
        .padding(horizontal = 12.dp)
        .clip(shape)
        .background(MaterialTheme.colorScheme.surface)
        .border(
            width = 2.dp,
            color = borderColor,
            shape = shape,
        )
}
