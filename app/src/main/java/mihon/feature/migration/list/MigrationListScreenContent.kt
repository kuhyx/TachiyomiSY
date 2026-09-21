package mihon.feature.migration.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.CopyAll
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.util.animateItemFastScroll
import mihon.feature.migration.list.models.MigratingManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.components.material.topSmallPaddingValues
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.plus

private const val ACTION_COLUMN_WEIGHT = 0.2f

@Composable
internal fun MigrationListScreenContent(
    items: List<MigratingManga>,
    migrationComplete: Boolean,
    finishedCount: Int,
    onItemClick: (Manga) -> Unit,
    onSearchManually: (MigratingManga) -> Unit,
    onSkip: (Long) -> Unit,
    onMigrate: (Long) -> Unit,
    onCopy: (Long) -> Unit,
    openMigrationDialog: (Boolean) -> Unit,
) {
    Scaffold(
        topBar = { scrollBehavior ->
            AppBar(
                title = if (items.isNotEmpty()) {
                    stringResource(MR.strings.migrationListScreenTitleWithProgress, finishedCount, items.size)
                } else {
                    stringResource(MR.strings.migrationListScreenTitle)
                },
                actions = { MigrationActions(items.size, migrationComplete, openMigrationDialog) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { contentPadding ->
        FastScrollLazyColumn(contentPadding = contentPadding + topSmallPaddingValues) {
            items(items, key = { it.manga.id }) { item ->
                MigrationRow(
                    item = item,
                    onItemClick = onItemClick,
                    onSearchManually = { onSearchManually(item) },
                    onSkip = { onSkip(item.manga.id) },
                    onMigrate = { onMigrate(item.manga.id) },
                    onCopy = { onCopy(item.manga.id) },
                )
            }
        }
    }
}

@Composable
private fun MigrationActions(itemCount: Int, migrationComplete: Boolean, openMigrationDialog: (Boolean) -> Unit) {
    AppBarActions(
        listOf(
            AppBar.Action(
                title = stringResource(MR.strings.migrationListScreen_copyActionLabel),
                icon = if (itemCount == 1) Icons.Outlined.ContentCopy else Icons.Outlined.CopyAll,
                onClick = { openMigrationDialog(true) },
                enabled = migrationComplete,
            ),
            AppBar.Action(
                title = stringResource(MR.strings.migrationListScreen_migrateActionLabel),
                icon = if (itemCount == 1) Icons.Outlined.Done else Icons.Outlined.DoneAll,
                onClick = { openMigrationDialog(false) },
                enabled = migrationComplete,
            ),
        ),
    )
}

// One entry: the current manga, an arrow, the match found for it, and the actions menu.
@Composable
private fun LazyItemScope.MigrationRow(
    item: MigratingManga,
    onItemClick: (Manga) -> Unit,
    onSearchManually: () -> Unit,
    onSkip: () -> Unit,
    onMigrate: () -> Unit,
    onCopy: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .animateItemFastScroll()
            .padding(
                start = MaterialTheme.padding.medium,
                end = MaterialTheme.padding.small,
            )
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MigrationListItem(
            modifier = Modifier
                .weight(1f)
                .align(Alignment.Top)
                .fillMaxHeight(),
            manga = item.manga,
            source = item.source,
            chapterCount = item.chapterCount,
            latestChapter = item.latestChapter,
            onClick = { onItemClick(item.manga) },
        )
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = null,
            modifier = Modifier.weight(ACTION_COLUMN_WEIGHT),
        )
        val result by item.searchResult.collectAsState()
        MigrationListItemResult(
            modifier = Modifier
                .weight(1f)
                .align(Alignment.Top)
                .fillMaxHeight(),
            result = result,
            onItemClick = onItemClick,
        )
        MigrationListItemAction(
            modifier = Modifier.weight(ACTION_COLUMN_WEIGHT),
            result = result,
            onSearchManually = onSearchManually,
            onSkip = onSkip,
            onMigrate = onMigrate,
            onCopy = onCopy,
        )
    }
}
