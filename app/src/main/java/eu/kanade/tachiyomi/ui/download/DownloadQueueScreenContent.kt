package eu.kanade.tachiyomi.ui.download

import android.view.LayoutInflater
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallExtendedFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.presentation.components.NestedMenuItem
import eu.kanade.tachiyomi.databinding.DownloadListBinding
import tachiyomi.core.common.util.lang.launchUI
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.Pill
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.roundToInt

private const val PILL_ALPHA_DARK = 0.12f
private const val PILL_ALPHA_LIGHT = 0.08f

// The sections of [DownloadQueueScreen]: title with the queue count, sort menu, pause/resume FAB and the list.

@Composable
internal fun DownloadQueueTitle(downloadCount: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(MR.strings.label_download_queue),
            maxLines = 1,
            modifier = Modifier.weight(1f, false),
            overflow = TextOverflow.Ellipsis,
        )
        if (downloadCount > 0) {
            val pillAlpha = if (isSystemInDarkTheme()) PILL_ALPHA_DARK else PILL_ALPHA_LIGHT
            Pill(
                text = "$downloadCount",
                modifier = Modifier.padding(start = 4.dp),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = pillAlpha),
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
internal fun DownloadQueueActions(screenModel: DownloadQueueScreenModel) {
    var sortExpanded by remember { mutableStateOf(false) }
    DropdownMenu(
        expanded = sortExpanded,
        onDismissRequest = { sortExpanded = false },
    ) {
        NestedMenuItem(
            text = { Text(text = stringResource(MR.strings.action_order_by_upload_date)) },
            children = { closeMenu ->
                SortMenuItem(MR.strings.action_newest, closeMenu) {
                    screenModel.reorderQueue({ it.download.chapter.dateUpload }, true)
                }
                SortMenuItem(MR.strings.action_oldest, closeMenu) {
                    screenModel.reorderQueue({ it.download.chapter.dateUpload }, false)
                }
            },
        )
        NestedMenuItem(
            text = { Text(text = stringResource(MR.strings.action_order_by_chapter_number)) },
            children = { closeMenu ->
                SortMenuItem(MR.strings.action_asc, closeMenu) {
                    screenModel.reorderQueue({ it.download.chapter.chapterNumber }, false)
                }
                SortMenuItem(MR.strings.action_desc, closeMenu) {
                    screenModel.reorderQueue({ it.download.chapter.chapterNumber }, true)
                }
            },
        )
    }
    AppBarActions(
        listOf(
            AppBar.Action(
                title = stringResource(MR.strings.action_sort),
                icon = Icons.AutoMirrored.Outlined.Sort,
                onClick = { sortExpanded = true },
            ),
            AppBar.OverflowAction(
                title = stringResource(MR.strings.action_cancel_all),
                onClick = { screenModel.clearQueue() },
            ),
        ),
    )
}

@Composable
private fun SortMenuItem(label: dev.icerock.moko.resources.StringResource, closeMenu: () -> Unit, reorder: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text = stringResource(label)) },
        onClick = {
            reorder()
            closeMenu()
        },
    )
}

@Composable
internal fun PauseResumeFab(screenModel: DownloadQueueScreenModel, expanded: Boolean, visible: Boolean) {
    val isRunning by screenModel.isDownloaderRunning.collectAsState()
    SmallExtendedFloatingActionButton(
        text = {
            val id = if (isRunning) MR.strings.action_pause else MR.strings.action_resume
            Text(text = stringResource(id))
        },
        icon = {
            val icon = if (isRunning) Icons.Outlined.Pause else Icons.Filled.PlayArrow
            Icon(imageVector = icon, contentDescription = null)
        },
        onClick = {
            if (isRunning) {
                screenModel.pauseDownloads()
            } else {
                screenModel.startDownloads()
            }
        },
        expanded = expanded,
        modifier = Modifier.animateFloatingActionButton(
            visible = visible,
            alignment = Alignment.BottomEnd,
        ),
    )
}

// The queue itself is still a RecyclerView (drag handles); the padding is pushed into the view in pixels.
@Composable
internal fun DownloadQueueList(
    screenModel: DownloadQueueScreenModel,
    downloadList: List<DownloadHeaderItem>,
    contentPadding: PaddingValues,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val left = with(density) { contentPadding.calculateLeftPadding(layoutDirection).toPx().roundToInt() }
    val top = with(density) { contentPadding.calculateTopPadding().toPx().roundToInt() }
    val right = with(density) { contentPadding.calculateRightPadding(layoutDirection).toPx().roundToInt() }
    val bottom = with(density) { contentPadding.calculateBottomPadding().toPx().roundToInt() }
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { context ->
            val binding = DownloadListBinding.inflate(LayoutInflater.from(context))
            val adapter = DownloadAdapter(screenModel.listener)
            screenModel.controllerBinding = binding
            screenModel.adapter = adapter
            binding.root.adapter = adapter
            adapter.isHandleDragEnabled = true
            binding.root.layoutManager = LinearLayoutManager(context)
            ViewCompat.setNestedScrollingEnabled(binding.root, true)
            scope.launchUI {
                screenModel.getDownloadStatusFlow().collect(screenModel::onStatusChange)
            }
            scope.launchUI {
                screenModel.getDownloadProgressFlow().collect(screenModel::onUpdateDownloadedPages)
            }
            binding.root
        },
        update = { view ->
            view.updatePadding(left = left, top = top, right = right, bottom = bottom)
            // The factory above gave the list this adapter; the model's reference may already be cleared.
            (view.adapter as DownloadAdapter).updateDataSet(downloadList)
        },
    )
}
