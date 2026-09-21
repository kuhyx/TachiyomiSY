package eu.kanade.presentation.manga.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.BookmarkRemove
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.RemoveDone
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.components.DownloadDropdownMenu
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.presentation.manga.DownloadAction
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.system.isTabletUi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.time.Duration.Companion.seconds

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

// A menu button wired to its long-press confirmation slot.
@Composable
private fun RowScope.SlotButton(
    slots: ConfirmSlots,
    slot: Enum<*>,
    label: StringResource,
    icon: ImageVector,
    content: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    Button(
        title = stringResource(label),
        icon = icon,
        toConfirm = slots.confirm[slot.ordinal],
        onLongClick = { slots.onLongClickItem(slot.ordinal) },
        onClick = onClick,
        content = content,
    )
}

// Long-pressing a button widens it for a second as a "tap again to confirm" affordance; one slot per action.
private data class ConfirmSlots(val confirm: SnapshotStateList<Boolean>, val onLongClickItem: (Int) -> Unit)

@Composable
private fun rememberConfirmSlots(size: Int): ConfirmSlots {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val confirm = remember { List(size) { false }.toMutableStateList() }
    var resetJob by remember { mutableStateOf<Job?>(null) }
    return remember(confirm) {
        ConfirmSlots(confirm) { toConfirmIndex ->
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            confirm.indices.forEach { i -> confirm[i] = i == toConfirmIndex }
            resetJob?.cancel()
            resetJob = scope.launch {
                delay(1.seconds)
                if (isActive) confirm[toConfirmIndex] = false
            }
        }
    }
}

@Composable
private fun RowScope.Button(
    title: String,
    icon: ImageVector,
    toConfirm: Boolean,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
    content: (@Composable () -> Unit)? = null,
) {
    val animatedWeight by animateFloatAsState(
        targetValue = if (toConfirm) 2f else 1f,
        label = "weight",
    )
    Box(
        modifier = Modifier
            .size(48.dp)
            .weight(animatedWeight)
            .combinedClickable(
                interactionSource = null,
                indication = ripple(bounded = false),
                onLongClick = onLongClick,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
            )
            AnimatedVisibility(
                visible = toConfirm,
                enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
            ) {
                Text(
                    text = title,
                    overflow = TextOverflow.Visible,
                    maxLines = 1,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        content?.invoke()
    }
}

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
                if (onDownloadClicked != null) {
                    LibraryDownloadButton(slots, onDownloadClicked)
                }
                SlotButton(slots, LibraryAction.DELETE, MR.strings.action_delete, Icons.Outlined.Delete) {
                    onDeleteClicked()
                }
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
private fun rememberMarkUnreadInOverflow(): Boolean {
    val configuration = LocalConfiguration.current
    return remember { !configuration.isTabletUi() }
}

@Composable
private fun Modifier.bottomMenuPadding(): Modifier = this
    .windowInsetsPadding(
        WindowInsets.navigationBars
            .only(WindowInsetsSides.Bottom),
    )
    .padding(horizontal = 8.dp, vertical = 12.dp)

@Composable
private fun RowScope.LibraryDownloadButton(slots: ConfirmSlots, onDownloadClicked: (DownloadAction) -> Unit) {
    var downloadExpanded by remember { mutableStateOf(false) }
    SlotButton(
        slots,
        LibraryAction.DOWNLOAD,
        MR.strings.action_download,
        Icons.Outlined.Download,
        content = {
            DownloadDropdownMenu(
                expanded = downloadExpanded,
                onDismissRequest = { downloadExpanded = false },
                onDownloadClicked = onDownloadClicked,
                offset = BottomBarMenuDpOffset,
            )
        },
    ) {
        downloadExpanded = !downloadExpanded
    }
}

// SY -->
private data class LibraryOverflowActions(
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

@Composable
private fun RowScope.LibraryMoreButton(
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

/** The buttons of the library menu, in order; each has a long-press confirmation slot. */
private enum class LibraryAction {
    MOVE_CATEGORY,
    MARK_READ,
    MARK_UNREAD,
    DOWNLOAD,
    DELETE,
    MORE,
}

private val BottomBarMenuDpOffset = DpOffset(0.dp, 0.dp)
