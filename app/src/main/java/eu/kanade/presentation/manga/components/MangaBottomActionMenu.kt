package eu.kanade.presentation.manga.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.BookmarkRemove
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.RemoveDone
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.R
import tachiyomi.i18n.MR

@Composable
internal fun MangaBottomActionMenu(
    visible: Boolean,
    modifier: Modifier = Modifier,
    onBookmarkClicked: (() -> Unit)? = null,
    onRemoveBookmarkClicked: (() -> Unit)? = null,
    onMarkAsReadClicked: (() -> Unit)? = null,
    onMarkAsUnreadClicked: (() -> Unit)? = null,
    onMarkPreviousAsReadClicked: (() -> Unit)? = null,
    onDownloadClicked: (() -> Unit)? = null,
    onDeleteClicked: (() -> Unit)? = null,
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(expandFrom = Alignment.Bottom),
        exit = shrinkVertically(shrinkTowards = Alignment.Bottom),
    ) {
        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.large.copy(bottomEnd = ZeroCornerSize, bottomStart = ZeroCornerSize),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            val slots = rememberConfirmSlots(ChapterAction.entries.size)
            // Callbacks in ChapterAction order; a button shows only when its action applies to the selection.
            val callbacks = listOf(
                onBookmarkClicked,
                onRemoveBookmarkClicked,
                onMarkAsReadClicked,
                onMarkAsUnreadClicked,
                onMarkPreviousAsReadClicked,
                onDownloadClicked,
                onDeleteClicked,
            )
            Row(
                modifier = Modifier
                    .padding(
                        WindowInsets.navigationBars
                            .only(WindowInsetsSides.Bottom)
                            .asPaddingValues(),
                    )
                    .padding(horizontal = 8.dp, vertical = 12.dp),
            ) {
                ChapterAction.entries.zip(callbacks).forEach { (action, onClick) ->
                    if (onClick != null) {
                        SlotButton(slots, action, action.label, action.icon(), onClick = onClick)
                    }
                }
            }
        }
    }
}

// SY <--

private enum class ChapterAction(val label: StringResource) {
    BOOKMARK(MR.strings.action_bookmark),
    REMOVE_BOOKMARK(MR.strings.action_remove_bookmark),
    MARK_READ(MR.strings.action_mark_as_read),
    MARK_UNREAD(MR.strings.action_mark_as_unread),
    MARK_PREVIOUS_READ(MR.strings.action_mark_previous_as_read),
    DOWNLOAD(MR.strings.action_download),
    DELETE(MR.strings.action_delete),
}

// Resolved in composition because the "mark previous" glyph is a drawable resource.
@Composable
private fun ChapterAction.icon(): ImageVector = when (this) {
    ChapterAction.BOOKMARK -> Icons.Outlined.BookmarkAdd
    ChapterAction.REMOVE_BOOKMARK -> Icons.Outlined.BookmarkRemove
    ChapterAction.MARK_READ -> Icons.Outlined.DoneAll
    ChapterAction.MARK_UNREAD -> Icons.Outlined.RemoveDone
    ChapterAction.MARK_PREVIOUS_READ -> ImageVector.vectorResource(R.drawable.ic_done_prev_24dp)
    ChapterAction.DOWNLOAD -> Icons.Outlined.Download
    ChapterAction.DELETE -> Icons.Outlined.Delete
}
