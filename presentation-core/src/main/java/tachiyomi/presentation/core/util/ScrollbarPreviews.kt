package tachiyomi.presentation.core.util

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val PREVIEW_SIZE_DP = 400
private const val PREVIEW_ITEMS = 50
private val PreviewItemPadding: Dp = 16.dp
private val PreviewRowItemHorizontalPadding: Dp = 8.dp

/** A 50-item column with [drawVerticalScrollbar], for the IDE preview. */
@Preview(widthDp = PREVIEW_SIZE_DP, heightDp = PREVIEW_SIZE_DP, showBackground = true)
@Composable
public fun LazyListScrollbarPreview() {
    val state = rememberLazyListState()
    LazyColumn(
        modifier = Modifier.drawVerticalScrollbar(state),
        state = state,
    ) {
        items(PREVIEW_ITEMS) {
            Text(
                text = "Item ${it + 1}",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PreviewItemPadding),
            )
        }
    }
}

/** A 50-item row with [drawHorizontalScrollbar], for the IDE preview. */
@Preview(widthDp = PREVIEW_SIZE_DP, showBackground = true)
@Composable
public fun LazyRowScrollbarPreview() {
    val state = rememberLazyListState()
    LazyRow(
        modifier = Modifier.drawHorizontalScrollbar(state),
        state = state,
    ) {
        items(PREVIEW_ITEMS) {
            Text(
                text = (it + 1).toString(),
                modifier = Modifier
                    .padding(horizontal = PreviewRowItemHorizontalPadding, vertical = PreviewItemPadding),
            )
        }
    }
}
