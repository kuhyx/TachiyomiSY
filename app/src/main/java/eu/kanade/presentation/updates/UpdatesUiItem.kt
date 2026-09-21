package eu.kanade.presentation.updates

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import eu.kanade.presentation.components.relativeDateText
import eu.kanade.presentation.manga.components.ChapterDownloadAction
import eu.kanade.presentation.util.animateItemFastScroll
import eu.kanade.presentation.util.relativeTimeSpanString
import eu.kanade.tachiyomi.ui.updates.UpdatesItem
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.ListGroupHeader
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

internal fun LazyListScope.updatesLastUpdatedItem(
    lastUpdated: Long,
) {
    item(key = "updates-lastUpdated") {
        Box(
            modifier = Modifier
                .animateItem(fadeInSpec = null, fadeOutSpec = null)
                .padding(horizontal = MaterialTheme.padding.medium, vertical = MaterialTheme.padding.small),
        ) {
            Text(
                text = stringResource(MR.strings.updates_last_update_info, relativeTimeSpanString(lastUpdated)),
                fontStyle = FontStyle.Italic,
            )
        }
    }
}

internal fun LazyListScope.updatesUiItems(
    uiModels: List<UpdatesUiModel>,
    selectionMode: Boolean,
    // SY -->
    preserveReadingPosition: Boolean,
    // SY <--
    onUpdateSelected: (UpdatesItem, Boolean, Boolean) -> Unit,
    onClickCover: (UpdatesItem) -> Unit,
    onClickUpdate: (UpdatesItem) -> Unit,
    onDownloadChapter: (List<UpdatesItem>, ChapterDownloadAction) -> Unit,
) {
    items(
        items = uiModels,
        contentType = {
            when (it) {
                is UpdatesUiModel.Header -> "header"
                is UpdatesUiModel.Item -> "item"
            }
        },
        key = {
            when (it) {
                is UpdatesUiModel.Header -> "updatesHeader-${it.hashCode()}"
                is UpdatesUiModel.Item -> "updates-${it.item.update.mangaId}-${it.item.update.chapterId}"
            }
        },
    ) { item ->
        when (item) {
            is UpdatesUiModel.Header -> {
                ListGroupHeader(
                    modifier = Modifier.animateItemFastScroll(),
                    text = relativeDateText(item.date),
                )
            }
            is UpdatesUiModel.Item -> {
                UpdatesUiItemRow(
                    updatesItem = item.item,
                    selectionMode = selectionMode,
                    preserveReadingPosition = preserveReadingPosition,
                    onUpdateSelected = onUpdateSelected,
                    onClickCover = onClickCover,
                    onClickUpdate = onClickUpdate,
                    onDownloadChapter = onDownloadChapter,
                )
            }
        }
    }
}
