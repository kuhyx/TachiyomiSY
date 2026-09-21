package eu.kanade.presentation.manga.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.DisabledByDefault
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.TextButton
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun ScanlatorFilterDialog(
    availableScanlators: Set<String>,
    excludedScanlators: Set<String>,
    onDismissRequest: () -> Unit,
    onConfirm: (Set<String>) -> Unit,
) {
    val sortedAvailableScanlators = remember(availableScanlators) {
        availableScanlators.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it })
    }
    val mutableExcludedScanlators = remember(excludedScanlators) { excludedScanlators.toMutableStateList() }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = stringResource(MR.strings.exclude_scanlators)) },
        text = {
            if (sortedAvailableScanlators.isEmpty()) {
                Text(text = stringResource(MR.strings.no_scanlators_found))
            } else {
                ScanlatorList(sortedAvailableScanlators, mutableExcludedScanlators)
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = true,
        ),
        confirmButton = {
            if (sortedAvailableScanlators.isEmpty()) {
                TextButton(onClick = onDismissRequest) {
                    Text(text = stringResource(MR.strings.action_cancel))
                }
            } else {
                ScanlatorButtons(availableScanlators, mutableExcludedScanlators, onDismissRequest, onConfirm)
            }
        },
    )
}

// Tapping a scanlator toggles it in [excluded]; scroll shadows mark overflow at either end.
@Composable
private fun ScanlatorList(scanlators: List<String>, excluded: SnapshotStateList<String>) {
    Box {
        val state = rememberLazyListState()
        LazyColumn(state = state) {
            items(scanlators) { scanlator ->
                val isExcluded = excluded.contains(scanlator)
                ScanlatorRow(
                    scanlator = scanlator,
                    isExcluded = isExcluded,
                    onClick = { if (isExcluded) excluded.remove(scanlator) else excluded.add(scanlator) },
                )
            }
        }
        if (state.canScrollBackward) HorizontalDivider(modifier = Modifier.align(Alignment.TopCenter))
        if (state.canScrollForward) HorizontalDivider(modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun ScanlatorRow(scanlator: String, isExcluded: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .clip(MaterialTheme.shapes.small)
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.padding.small),
    ) {
        Icon(
            imageVector = if (isExcluded) {
                Icons.Rounded.DisabledByDefault
            } else {
                Icons.Rounded.CheckBoxOutlineBlank
            },
            tint = if (isExcluded) MaterialTheme.colorScheme.primary else LocalContentColor.current,
            contentDescription = null,
        )
        Text(
            text = scanlator,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 24.dp),
        )
    }
}

@Composable
private fun ScanlatorButtons(
    availableScanlators: Set<String>,
    excluded: SnapshotStateList<String>,
    onDismissRequest: () -> Unit,
    onConfirm: (Set<String>) -> Unit,
) {
    FlowRow {
        if (excluded.isEmpty()) {
            TextButton(onClick = { excluded.addAll(availableScanlators) }) {
                Text(text = stringResource(MR.strings.action_select_all))
            }
        } else {
            TextButton(onClick = excluded::clear) {
                Text(text = stringResource(MR.strings.action_reset))
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        TextButton(onClick = onDismissRequest) {
            Text(text = stringResource(MR.strings.action_cancel))
        }
        TextButton(
            onClick = {
                onConfirm(excluded.toSet())
                onDismissRequest()
            },
        ) {
            Text(text = stringResource(MR.strings.action_ok))
        }
    }
}
