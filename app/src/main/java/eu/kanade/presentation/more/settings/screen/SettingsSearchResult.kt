package eu.kanade.presentation.more.settings.screen

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen

@Composable
internal fun SearchResult(
    searchKey: String,
    modifier: Modifier = Modifier,
    // The one caller passes both, so neither has a default.
    listState: LazyListState,
    contentPadding: PaddingValues,
    onItemClick: (SearchResultItem) -> Unit,
) {
    if (searchKey.isEmpty()) return

    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr

    val index = getIndex()
    val result by produceState<List<SearchResultItem>?>(initialValue = null, searchKey) {
        value = searchIndex(index, searchKey, isLtr)
    }

    Crossfade(targetState = result) {
        when {
            it == null -> {}
            it.isEmpty() -> {
                EmptyScreen(stringResource(MR.strings.no_results_found))
            }
            else -> {
                LazyColumn(
                    modifier = modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = contentPadding,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    items(
                        items = it,
                        key = { i -> i.hashCode() },
                    ) { item ->
                        SearchResultRow(item = item, onClick = { onItemClick(item) })
                    }
                }
            }
        }
    }
}

@Composable
internal fun SearchResultRow(item: SearchResultItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
    ) {
        Text(
            text = item.title,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            fontWeight = FontWeight.Normal,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = item.breadcrumbs,
            modifier = Modifier.paddingFromBaseline(top = 16.dp),
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
