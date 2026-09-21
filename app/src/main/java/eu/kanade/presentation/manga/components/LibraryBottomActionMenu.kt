package eu.kanade.presentation.manga.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.RemoveDone
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.DownloadDropdownMenu
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.presentation.manga.DownloadAction
import eu.kanade.tachiyomi.util.system.isTabletUi
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun LibraryBottomActionMenu(
    visible: Boolean,
    onChangeCategoryClicked: () -> Unit,
    onMarkAsReadClicked: () -> Unit,
    onMarkAsUnreadClicked: () -> Unit,
    onDownloadClicked: ((DownloadAction) -> Unit)?,
    onDeleteClicked: () -> Unit,
    onMigrateClicked: (() -> Unit)?,
    // SY -->
    onClickCleanTitles: (() -> Unit)?,
    onClickCollectRecommendations: (() -> Unit)?,
    onClickAddToMangaDex: (() -> Unit)?,
    onClickResetInfo: (() -> Unit)?,
    // SY <--
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(animationSpec = tween(delayMillis = 300)),
        exit = shrinkVertically(animationSpec = tween()),
    ) {
        Surface(
            modifier = modifier,
            shape = MaterialTheme.shapes.large.copy(bottomEnd = ZeroCornerSize, bottomStart = ZeroCornerSize),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            val slots = rememberConfirmSlots(LibraryAction.entries.size)
            // SY -->
            val overflow = LibraryOverflowActions(
                onClickCleanTitles = onClickCleanTitles,
                onMigrateClicked = onMigrateClicked,
                onClickCollectRecommendations = onClickCollectRecommendations,
                onClickAddToMangaDex = onClickAddToMangaDex,
                onClickResetInfo = onClickResetInfo,
            )
            val moveMarkPrev = rememberMarkUnreadInOverflow()
            // SY <--
            Row(modifier = Modifier.bottomMenuPadding()) {
                SlotButton(
                    slots,
                    LibraryAction.MOVE_CATEGORY,
                    MR.strings.action_move_category,
                    Icons.AutoMirrored.Outlined.Label,
                    onClick = onChangeCategoryClicked,
                )
                onDownloadClicked?.let { LibraryDownloadButton(slots, it) }
                SlotButton(
                    slots,
                    LibraryAction.DELETE,
                    MR.strings.action_delete,
                    Icons.Outlined.Delete,
                    onClick = onDeleteClicked,
                )
                // SY -->
                SlotButton(
                    slots,
                    LibraryAction.MARK_READ,
                    MR.strings.action_mark_as_read,
                    Icons.Outlined.DoneAll,
                    onClick = onMarkAsReadClicked,
                )
                if (overflow.isEmpty || !moveMarkPrev) {
                    SlotButton(
                        slots,
                        LibraryAction.MARK_UNREAD,
                        MR.strings.action_mark_as_unread,
                        Icons.Outlined.RemoveDone,
                        onClick = onMarkAsUnreadClicked,
                    )
                }
                if (!overflow.isEmpty) {
                    LibraryMoreButton(slots, overflow, onMarkAsUnreadClicked.takeIf { moveMarkPrev })
                }
                // SY <--
            }
        }
    }
}

// Phones move "mark as unread" into the overflow to make room for the More button.
@Composable
internal fun rememberMarkUnreadInOverflow(): Boolean {
    val configuration = LocalConfiguration.current
    return remember { !configuration.isTabletUi() }
}

@Composable
internal fun RowScope.LibraryDownloadButton(slots: ConfirmSlots, onDownloadClicked: (DownloadAction) -> Unit) {
    var downloadExpanded by remember { mutableStateOf(false) }
    SlotButton(
        slots,
        LibraryAction.DOWNLOAD,
        MR.strings.action_download,
        Icons.Outlined.Download,
        onClick = { downloadExpanded = !downloadExpanded },
    ) {
        DownloadDropdownMenu(
            expanded = downloadExpanded,
            onDismissRequest = { downloadExpanded = false },
            onDownloadClicked = onDownloadClicked,
            offset = BottomBarMenuDpOffset,
        )
    }
}

@Composable
internal fun RowScope.LibraryMoreButton(
    slots: ConfirmSlots,
    overflow: LibraryOverflowActions,
    onMarkAsUnreadClicked: (() -> Unit)?,
) {
    var overFlowOpen by remember { mutableStateOf(false) }
    Button(
        title = stringResource(MR.strings.label_more),
        icon = Icons.Outlined.MoreVert,
        toConfirm = slots.confirm[LibraryAction.MORE.ordinal],
        onLongClick = { slots.onLongClickItem(LibraryAction.MORE.ordinal) },
        onClick = { overFlowOpen = true },
    )
    DropdownMenu(
        expanded = overFlowOpen,
        onDismissRequest = { overFlowOpen = false },
    ) {
        val entries = listOf(
            MR.strings.action_mark_as_unread to onMarkAsUnreadClicked,
            SYMR.strings.action_clean_titles to overflow.onClickCleanTitles,
            MR.strings.migrate to overflow.onMigrateClicked,
            SYMR.strings.rec_search_short to overflow.onClickCollectRecommendations,
            SYMR.strings.mangadex_add_to_follows to overflow.onClickAddToMangaDex,
            SYMR.strings.reset_info to overflow.onClickResetInfo,
        )
        entries.forEach { (label, onClick) ->
            if (onClick != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(label)) },
                    onClick = onClick,
                )
            }
        }
    }
}

// SY -->
internal data class LibraryOverflowActions(
    val onClickCleanTitles: (() -> Unit)?,
    val onMigrateClicked: (() -> Unit)?,
    val onClickCollectRecommendations: (() -> Unit)?,
    val onClickAddToMangaDex: (() -> Unit)?,
    val onClickResetInfo: (() -> Unit)?,
) {
    val isEmpty: Boolean
        get() = onClickCleanTitles == null &&
            onClickAddToMangaDex == null &&
            onClickResetInfo == null &&
            onClickCollectRecommendations == null &&
            onMigrateClicked == null
}

/** The buttons of the library menu, in order; each has a long-press confirmation slot. */
internal enum class LibraryAction {
    MOVE_CATEGORY,
    MARK_READ,
    MARK_UNREAD,
    DOWNLOAD,
    DELETE,
    MORE,
}

private val BottomBarMenuDpOffset = DpOffset(0.dp, 0.dp)
