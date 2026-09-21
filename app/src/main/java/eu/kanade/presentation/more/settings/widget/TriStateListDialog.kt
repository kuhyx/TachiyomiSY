package eu.kanade.presentation.more.settings.widget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.DisabledByDefault
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

private enum class State(val icon: ImageVector, val description: StringResource) {
    CHECKED(Icons.Rounded.CheckBox, MR.strings.selected),
    INVERSED(Icons.Rounded.DisabledByDefault, MR.strings.disabled),
    UNCHECKED(Icons.Rounded.CheckBoxOutlineBlank, MR.strings.not_selected),
    ;

    // Tap cycle: unchecked -> checked (include) -> inversed (exclude) -> unchecked.
    fun next(): State = when (this) {
        UNCHECKED -> CHECKED
        CHECKED -> INVERSED
        INVERSED -> UNCHECKED
    }
}

@Composable
internal fun <T> TriStateListDialog(
    title: String,
    message: String? = null,
    items: List<T>,
    initialChecked: List<T>,
    initialInversed: List<T>,
    onDismissRequest: () -> Unit,
    onValueChanged: (newIncluded: List<T>, newExcluded: List<T>) -> Unit,
    itemLabel: @Composable (T) -> String,
) {
    val selected = remember {
        items
            .map {
                when (it) {
                    in initialChecked -> State.CHECKED
                    in initialInversed -> State.INVERSED
                    else -> State.UNCHECKED
                }
            }
            .toMutableStateList()
    }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = title) },
        text = {
            Column {
                if (message != null) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                TriStateList(items = items, selected = selected, itemLabel = itemLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onValueChanged(items.withState(selected, State.CHECKED), items.withState(selected, State.INVERSED))
                },
            ) {
                Text(text = stringResource(MR.strings.action_ok))
            }
        },
    )
}

private fun <T> List<T>.withState(selected: List<State>, state: State): List<T> =
    mapIndexedNotNull { index, category -> if (selected[index] == state) category else null }

@Composable
private fun <T> TriStateList(
    items: List<T>,
    selected: SnapshotStateList<State>,
    itemLabel: @Composable (T) -> String,
) {
    Box {
        val listState = rememberLazyListState()
        LazyColumn(state = listState) {
            itemsIndexed(items = items) { index, item ->
                TriStateRow(
                    state = selected[index],
                    label = itemLabel(item),
                    onClick = { selected[index] = selected[index].next() },
                )
            }
        }

        if (listState.canScrollBackward) HorizontalDivider(modifier = Modifier.align(Alignment.TopCenter))
        if (listState.canScrollForward) HorizontalDivider(modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun TriStateRow(state: State, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .defaultMinSize(minHeight = 48.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.padding(end = 20.dp),
            imageVector = state.icon,
            tint = if (state == State.UNCHECKED) {
                LocalContentColor.current
            } else {
                MaterialTheme.colorScheme.primary
            },
            contentDescription = stringResource(state.description),
        )
        Text(text = label)
    }
}
