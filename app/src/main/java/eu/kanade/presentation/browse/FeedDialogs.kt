package eu.kanade.presentation.browse

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.source.Source
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.domain.source.model.FeedSavedSearch
import tachiyomi.domain.source.model.SavedSearch
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.plus

@Composable
internal fun FeedAddDialog(
    sources: List<Source>,
    onDismiss: () -> Unit,
    onClickAdd: (Source?) -> Unit,
) {
    var selected by remember { mutableStateOf<Int?>(null) }
    AlertDialog(
        title = {
            Text(text = stringResource(SYMR.strings.feed))
        },
        text = {
            RadioSelector(options = sources, selected = selected) {
                selected = it
            }
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onClickAdd(selected?.let { sources[it] }) }) {
                Text(text = stringResource(MR.strings.action_ok))
            }
        },
    )
}

@Composable
internal fun FeedAddSearchDialog(
    source: Source,
    savedSearches: List<SavedSearch?>,
    onDismiss: () -> Unit,
    onClickAdd: (Source, SavedSearch?) -> Unit,
) {
    var selected by remember { mutableStateOf<Int?>(null) }
    AlertDialog(
        title = {
            Text(text = source.name)
        },
        text = {
            val context = LocalContext.current
            val savedSearchStrings = remember {
                savedSearches.map {
                    it?.name ?: context.stringResource(MR.strings.latest)
                }
            }
            RadioSelector(
                options = savedSearches,
                optionStrings = savedSearchStrings,
                selected = selected,
            ) {
                selected = it
            }
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onClickAdd(source, selected?.let { savedSearches[it] }) }) {
                Text(text = stringResource(MR.strings.action_ok))
            }
        },
    )
}

@Composable
internal fun <T> RadioSelector(
    options: List<T>,
    optionStrings: List<String> = remember { options.map { it.toString() } },
    selected: Int?,
    onSelectOption: (Int) -> Unit,
) {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        optionStrings.forEachIndexed { index, option ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clickable { onSelectOption(index) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected == index, onClick = null)
                Spacer(Modifier.width(4.dp))
                Text(option, maxLines = 1)
            }
        }
    }
}

@Composable
internal fun FeedDeleteConfirmDialog(
    feed: FeedSavedSearch,
    onDismiss: () -> Unit,
    onClickDeleteConfirm: (FeedSavedSearch) -> Unit,
) {
    AlertDialog(
        title = {
            Text(text = stringResource(SYMR.strings.feed))
        },
        text = {
            Text(text = stringResource(SYMR.strings.feed_delete))
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onClickDeleteConfirm(feed) }) {
                Text(text = stringResource(MR.strings.action_delete))
            }
        },
    )
}
