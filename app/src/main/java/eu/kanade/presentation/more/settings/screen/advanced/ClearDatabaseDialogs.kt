package eu.kanade.presentation.more.settings.screen.advanced

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FlipToBack
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.browse.components.SourceIcon
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.tachiyomi.util.system.toast
import tachiyomi.core.common.util.lang.launchUI
import tachiyomi.domain.source.model.Source
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.selectedBackground
import uy.kohesive.injekt.api.get

@Composable
internal fun SelectionActions(model: ClearDatabaseScreenModel) {
    AppBarActions(
        actions = listOf(
            AppBar.Action(
                title = stringResource(MR.strings.action_select_all),
                icon = Icons.Outlined.SelectAll,
                onClick = model::selectAll,
            ),
            AppBar.Action(
                title = stringResource(MR.strings.action_select_inverse),
                icon = Icons.Outlined.FlipToBack,
                onClick = model::invertSelection,
            ),
        ),
    )
}

// The keep-read switch defaults on; turning it off also wipes the history of what is removed.
@Composable
internal fun ConfirmClearDialog(model: ClearDatabaseScreenModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var keepReadManga by remember { mutableStateOf(true) }
    AlertDialog(
        title = {
            Text(text = stringResource(MR.strings.are_you_sure))
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            ) {
                Text(text = stringResource(MR.strings.clear_database_text))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(MR.strings.clear_db_exclude_read),
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = keepReadManga,
                        onCheckedChange = { keepReadManga = it },
                    )
                }
                if (!keepReadManga) {
                    Text(
                        text = stringResource(MR.strings.clear_database_history_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        onDismissRequest = model::hideConfirmation,
        confirmButton = {
            TextButton(
                onClick = {
                    scope.launchUI {
                        model.removeMangaBySourceId(keepReadManga)
                        model.clearSelection()
                        model.hideConfirmation()
                        context.toast(MR.strings.clear_database_completed)
                    }
                },
            ) {
                Text(text = stringResource(MR.strings.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = model::hideConfirmation) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
    )
}

@Composable
internal fun ClearDatabaseItem(
    source: Source,
    count: Long,
    isSelected: Boolean,
    onClickSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .selectedBackground(isSelected)
            .clickable(onClick = onClickSelect)
            .padding(horizontal = 8.dp)
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SourceIcon(source = source)
        Column(
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f),
        ) {
            Text(
                text = source.visualName,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(text = stringResource(MR.strings.clear_database_source_item_count, count))
        }
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onClickSelect() },
        )
    }
}
