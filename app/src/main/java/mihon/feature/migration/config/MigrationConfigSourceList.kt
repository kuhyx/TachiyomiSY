package mihon.feature.migration.config

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Deselect
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import eu.kanade.presentation.browse.components.SourceIcon
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.tachiyomi.util.system.LocaleHelper
import mihon.feature.migration.config.MigrationConfigScreen.MigrationSource
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState
import sh.calvin.reorderable.rememberReorderableLazyListState
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.components.Pill
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.api.get

@Composable
internal fun SelectionActions(screenModel: MigrationConfigScreenModel) {
    AppBarActions(
        listOf(
            AppBar.Action(
                title = stringResource(MR.strings.migrationConfigScreen_selectAllLabel),
                icon = Icons.Outlined.SelectAll,
                onClick = { screenModel.toggleSelection(MigrationConfigScreenModel.SelectionConfig.All) },
            ),
            AppBar.Action(
                title = stringResource(MR.strings.migrationConfigScreen_selectNoneLabel),
                icon = Icons.Outlined.Deselect,
                onClick = { screenModel.toggleSelection(MigrationConfigScreenModel.SelectionConfig.None) },
            ),
            AppBar.OverflowAction(
                title = stringResource(MR.strings.migrationConfigScreen_selectEnabledLabel),
                onClick = { screenModel.toggleSelection(MigrationConfigScreenModel.SelectionConfig.Enabled) },
            ),
            AppBar.OverflowAction(
                title = stringResource(MR.strings.migrationConfigScreen_selectPinnedLabel),
                onClick = { screenModel.toggleSelection(MigrationConfigScreenModel.SelectionConfig.Pinned) },
            ),
        ),
    )
}

// The selected sources (reorderable) above the available ones, each under its header.
@Composable
internal fun SourceLists(
    screenModel: MigrationConfigScreenModel,
    selectedSources: List<MigrationSource>,
    availableSources: List<MigrationSource>,
    showLanguage: Boolean,
    lazyListState: LazyListState,
    contentPadding: PaddingValues,
) {
    val reorderableState = rememberReorderableLazyListState(lazyListState, contentPadding) { from, to ->
        val fromIndex = selectedSources.indexOfFirst { it.id == from.key }
        val toIndex = selectedSources.indexOfFirst { it.id == to.key }
        if (!(fromIndex == -1 || toIndex == -1)) {
            screenModel.orderSource(fromIndex, toIndex)
        }
    }
    FastScrollLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = lazyListState,
        contentPadding = contentPadding,
    ) {
        listOf(selectedSources, availableSources).fastForEachIndexed { listIndex, sources ->
            val selectedSourceList = listIndex == 0
            if (sources.isNotEmpty()) {
                sourceHeader(selected = selectedSourceList)
            }
            itemsIndexed(
                items = sources,
                key = { _, item -> item.id },
            ) { index, item ->
                SourceItemContainer(
                    firstItem = index == 0,
                    lastItem = index == sources.size - 1,
                    source = item,
                    showLanguage = showLanguage,
                    dragEnabled = selectedSourceList && sources.size > 1,
                    state = reorderableState,
                    key = { if (selectedSourceList) it.id else "available-${it.id}" },
                    onClick = { screenModel.toggleSelection(item.id) },
                )
            }
        }
    }
}

@Composable
internal fun SourceItem(
    source: MigrationSource,
    showLanguage: Boolean,
    dragEnabled: Boolean,
    scope: ReorderableCollectionItemScope,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        trailingContent = if (dragEnabled) {
            {
                Icon(
                    imageVector = Icons.Outlined.DragHandle,
                    contentDescription = null,
                    modifier = with(scope) {
                        Modifier.draggableHandle()
                    },
                )
            }
        } else {
            null
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SourceIcon(source = source.source)
            Text(
                text = source.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            if (showLanguage) {
                Pill(
                    text = LocaleHelper.getShortDisplayName(source.shortLanguage, uppercase = true),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
internal fun LazyItemScope.SourceItemContainer(
    firstItem: Boolean,
    lastItem: Boolean,
    source: MigrationSource,
    showLanguage: Boolean,
    dragEnabled: Boolean,
    state: ReorderableLazyListState,
    key: (MigrationSource) -> Any,
    onClick: () -> Unit,
) {
    val shape = remember(firstItem, lastItem) {
        val top = if (firstItem) 12.dp else 0.dp
        val bottom = if (lastItem) 12.dp else 0.dp
        RoundedCornerShape(top, top, bottom, bottom)
    }

    ReorderableItem(
        state = state,
        key = key(source),
        enabled = dragEnabled,
    ) { _ ->
        val itemScope = this
        ElevatedCard(
            shape = shape,
            modifier = Modifier
                .padding(horizontal = MaterialTheme.padding.medium)
                .animateItem(),
        ) {
            SourceItem(
                source = source,
                showLanguage = showLanguage,
                dragEnabled = dragEnabled,
                scope = itemScope,
                onClick = onClick,
            )
        }
    }

    if (!lastItem) {
        HorizontalDivider(modifier = Modifier.padding(horizontal = MaterialTheme.padding.medium))
    }
}
