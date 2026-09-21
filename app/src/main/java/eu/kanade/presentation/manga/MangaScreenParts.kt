package eu.kanade.presentation.manga

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastMap
import eu.kanade.presentation.components.relativeDateText
import eu.kanade.presentation.manga.components.ChapterDownloadAction
import eu.kanade.presentation.manga.components.ChapterHeader
import eu.kanade.presentation.manga.components.MangaBottomActionMenu
import eu.kanade.presentation.manga.components.MangaChapterListItem
import eu.kanade.presentation.manga.components.MangaToolbar
import eu.kanade.presentation.manga.components.MissingChapterCountListItem
import eu.kanade.presentation.util.formatChapterNumber
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.manga.ChapterList
import eu.kanade.tachiyomi.ui.manga.MangaScreenModel
import eu.kanade.tachiyomi.ui.manga.MergedMangaData
import exh.metadata.MetadataUtil
import exh.source.MERGED_SOURCE_ID
import exh.source.isEhBasedManga
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.service.missingChaptersCount
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.displayMode
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.shouldExpandFAB
import tachiyomi.source.local.isLocal
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

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

internal fun LazyListScope.chapterHeaderItem(
    chapters: List<ChapterList.Item>,
    isAnySelected: Boolean,
    onClick: () -> Unit,
) {
    item(
        key = MangaScreenItem.CHAPTER_HEADER,
        contentType = MangaScreenItem.CHAPTER_HEADER,
    ) {
        val missingChapterCount = remember(chapters) {
            chapters.map { it.chapter.chapterNumber }.missingChaptersCount()
        }
        ChapterHeader(
            enabled = !isAnySelected,
            chapterCount = chapters.size,
            missingChapterCount = missingChapterCount,
            onClick = onClick,
        )
    }
}

internal fun LazyListScope.sharedChapterItems(
    state: MangaScreenModel.State.Success,
    chapters: List<ChapterList>,
    isAnyChapterSelected: Boolean,
    chapterSwipeStartAction: LibraryPreferences.ChapterSwipeAction,
    chapterSwipeEndAction: LibraryPreferences.ChapterSwipeAction,
    actions: ChapterRowActions,
) {
    items(
        items = chapters,
        key = { item ->
            when (item) {
                is ChapterList.MissingCount -> "missing-count-${item.id}"
                is ChapterList.Item -> "chapter-${item.id}"
            }
        },
        contentType = { MangaScreenItem.CHAPTER },
    ) { item ->
        when (item) {
            is ChapterList.MissingCount -> MissingChapterCountListItem(count = item.count)
            is ChapterList.Item -> ChapterRow(
                item = item,
                manga = state.manga,
                mergedData = state.mergedData,
                isAnyChapterSelected = isAnyChapterSelected,
                // SY -->
                alwaysShowReadingProgress = state.alwaysShowReadingProgress,
                // SY <--
                swipeActions = chapterSwipeStartAction to chapterSwipeEndAction,
                actions = actions,
            )
        }
    }
}

@Composable
private fun ChapterRow(
    item: ChapterList.Item,
    manga: Manga,
    mergedData: MergedMangaData?,
    isAnyChapterSelected: Boolean,
    alwaysShowReadingProgress: Boolean,
    swipeActions: Pair<LibraryPreferences.ChapterSwipeAction, LibraryPreferences.ChapterSwipeAction>,
    actions: ChapterRowActions,
) {
    val haptic = LocalHapticFeedback.current
    MangaChapterListItem(
        title = if (manga.displayMode == Manga.CHAPTER_DISPLAY_NUMBER) {
            stringResource(MR.strings.display_mode_chapter, formatChapterNumber(item.chapter.chapterNumber))
        } else {
            item.chapter.name
        },
        date = item.chapter.dateUpload.takeIf { it > 0L }?.let { chapterDateText(manga, it) },
        readProgress = item.chapter.lastPageRead
            .takeIf {
                /* SY --> */(!item.chapter.read || alwaysShowReadingProgress)/* SY <-- */ && it > 0L
            }
            ?.let { stringResource(MR.strings.chapter_progress, it + 1) },
        scanlator = item.chapter.scanlator.takeIf {
            !it.isNullOrBlank() /* SY --> */ && item.showScanlator /* SY <-- */
        },
        // SY -->
        sourceName = item.sourceName,
        // SY <--
        read = item.chapter.read,
        bookmark = item.chapter.bookmark,
        selected = item.selected,
        downloadIndicatorEnabled =
        !isAnyChapterSelected && !(mergedData?.manga?.get(item.chapter.mangaId) ?: manga).isLocal(),
        downloadStateProvider = { item.downloadState },
        downloadProgressProvider = { item.downloadProgress },
        chapterSwipeStartAction = swipeActions.first,
        chapterSwipeEndAction = swipeActions.second,
        onLongClick = {
            actions.onChapterSelected(item, !item.selected, true)
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        },
        onClick = {
            onChapterItemClick(
                chapterItem = item,
                isAnyChapterSelected = isAnyChapterSelected,
                onToggleSelection = { actions.onChapterSelected(item, !item.selected, false) },
                onChapterClicked = actions.onChapterClicked,
            )
        },
        onDownloadClick = actions.onDownloadChapter?.let { download ->
            { action: ChapterDownloadAction -> download(listOf(item), action) }
        },
        onChapterSwipe = { actions.onChapterSwipe(item, it) },
    )
}

// SY --> E-Hentai galleries carry an exact upload date, everything else shows a relative one.
@Composable
private fun chapterDateText(manga: Manga, dateUpload: Long): String {
    return if (manga.isEhBasedManga()) {
        MetadataUtil.EX_DATE_FORMAT
            .format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(dateUpload), ZoneId.systemDefault()))
    } else {
        relativeDateText(dateUpload)
    }
}
// SY <--

private fun onChapterItemClick(
    chapterItem: ChapterList.Item,
    isAnyChapterSelected: Boolean,
    onToggleSelection: (Boolean) -> Unit,
    onChapterClicked: (Chapter) -> Unit,
) {
    when {
        chapterItem.selected -> onToggleSelection(false)
        isAnyChapterSelected -> onToggleSelection(true)
        else -> onChapterClicked(chapterItem.chapter)
    }
}
