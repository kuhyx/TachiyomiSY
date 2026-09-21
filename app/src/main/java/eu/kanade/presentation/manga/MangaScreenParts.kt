package eu.kanade.presentation.manga

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallExtendedFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastMap
import eu.kanade.presentation.manga.components.ChapterDownloadAction
import eu.kanade.presentation.manga.components.MangaBottomActionMenu
import eu.kanade.presentation.manga.components.MangaToolbar
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.manga.ChapterList
import eu.kanade.tachiyomi.ui.manga.MangaScreenModel
import exh.source.MERGED_SOURCE_ID
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.shouldExpandFAB

// The pieces the phone and tablet manga layouts share.

@Composable
internal fun MangaScreenToolbar(
    state: MangaScreenModel.State.Success,
    chapters: List<ChapterList.Item>,
    actions: MangaScreenActions,
    titleAlphaProvider: () -> Float,
    backgroundAlphaProvider: () -> Float,
    modifier: Modifier = Modifier,
) {
    val selectedChapterCount = remember(chapters) { chapters.count { it.selected } }
    MangaToolbar(
        modifier = modifier,
        title = state.manga.title,
        hasFilters = state.filterActive,
        navigateUp = actions.toolbar.navigateUp,
        onClickFilter = actions.toolbar.onFilterButtonClicked,
        onClickShare = actions.toolbar.onShareClicked,
        onClickDownload = actions.toolbar.onDownloadActionClicked,
        onClickEditCategory = actions.header.onEditCategoryClicked,
        onClickRefresh = actions.toolbar.onRefresh,
        onClickMigrate = actions.toolbar.onMigrateClicked,
        onClickEditNotes = actions.info.onEditNotesClicked,
        // SY -->
        onClickEditInfo = actions.sy.onEditInfoClicked.takeIf { state.manga.favorite },
        onClickRecommend = actions.sy.onRecommendClicked.takeIf { state.showRecommendationsInOverflow },
        onClickMergedSettings =
        actions.sy.merge.onMergedSettingsClicked.takeIf { state.manga.source == MERGED_SOURCE_ID },
        onClickMerge = actions.sy.merge.onMergeClicked.takeIf { state.showMergeInOverflow },
        // SY <--
        actionModeCounter = selectedChapterCount,
        onCancelActionMode = { actions.selection.onAllChapterSelected(false) },
        onSelectAll = { actions.selection.onAllChapterSelected(true) },
        onInvertSelection = { actions.selection.onInvertSelection() },
        titleAlphaProvider = titleAlphaProvider,
        backgroundAlphaProvider = backgroundAlphaProvider,
    )
}

/** Start/resume reading; hidden while chapters are selected or everything is read. */
@Composable
internal fun ContinueReadingFab(
    state: MangaScreenModel.State.Success,
    chapters: List<ChapterList.Item>,
    isAnySelected: Boolean,
    chapterListState: LazyListState,
    onContinueReading: () -> Unit,
) {
    val isFABVisible = remember(chapters) {
        chapters.fastAny { !it.chapter.read } && !isAnySelected
    }
    SmallExtendedFloatingActionButton(
        text = {
            val isReading = remember(state.chapters) {
                state.chapters.fastAny { it.chapter.read }
            }
            Text(text = stringResource(if (isReading) MR.strings.action_resume else MR.strings.action_start))
        },
        icon = { Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null) },
        onClick = onContinueReading,
        expanded = chapterListState.shouldExpandFAB(),
        modifier = Modifier.animateFloatingActionButton(
            visible = isFABVisible,
            alignment = Alignment.BottomEnd,
        ),
    )
}

@Composable
internal fun SharedMangaBottomActionMenu(
    chapters: List<ChapterList.Item>,
    actions: MangaScreenActions,
    fillFraction: Float,
    modifier: Modifier = Modifier,
) {
    val selected = remember(chapters) { chapters.filter { it.selected } }
    val onDownloadChapter = actions.chapters.onDownloadChapter
    MangaBottomActionMenu(
        visible = selected.isNotEmpty(),
        modifier = modifier.fillMaxWidth(fillFraction),
        onBookmarkClicked = {
            actions.selection.onMultiBookmarkClicked.invoke(selected.fastMap { it.chapter }, true)
        }.takeIf { selected.fastAny { !it.chapter.bookmark } },
        onRemoveBookmarkClicked = {
            actions.selection.onMultiBookmarkClicked.invoke(selected.fastMap { it.chapter }, false)
        }.takeIf { selected.fastAll { it.chapter.bookmark } },
        onMarkAsReadClicked = {
            actions.selection.onMultiMarkAsReadClicked(selected.fastMap { it.chapter }, true)
        }.takeIf { selected.fastAny { !it.chapter.read } },
        onMarkAsUnreadClicked = {
            actions.selection.onMultiMarkAsReadClicked(selected.fastMap { it.chapter }, false)
        }.takeIf { selected.fastAny { it.chapter.read || it.chapter.lastPageRead > 0L } },
        onMarkPreviousAsReadClicked = {
            actions.selection.onMarkPreviousAsReadClicked(selected[0].chapter)
        }.takeIf { selected.size == 1 },
        onDownloadClicked = {
            onDownloadChapter!!(selected.toList(), ChapterDownloadAction.START)
        }.takeIf {
            onDownloadChapter != null && selected.fastAny { it.downloadState != Download.State.DOWNLOADED }
        },
        onDeleteClicked = {
            actions.selection.onMultiDeleteClicked(selected.fastMap { it.chapter })
        }.takeIf {
            selected.fastAny { it.downloadState == Download.State.DOWNLOADED }
        },
    )
}

// SY <--
