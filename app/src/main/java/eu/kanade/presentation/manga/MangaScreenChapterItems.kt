package eu.kanade.presentation.manga

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import eu.kanade.presentation.components.relativeDateText
import eu.kanade.presentation.manga.components.ChapterDownloadAction
import eu.kanade.presentation.manga.components.ChapterHeader
import eu.kanade.presentation.manga.components.MangaChapterListItem
import eu.kanade.presentation.manga.components.MissingChapterCountListItem
import eu.kanade.presentation.util.formatChapterNumber
import eu.kanade.tachiyomi.ui.manga.ChapterList
import eu.kanade.tachiyomi.ui.manga.MangaScreenModel
import eu.kanade.tachiyomi.ui.manga.MergedMangaData
import exh.metadata.MetadataUtil
import exh.source.isEhBasedManga
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.service.missingChaptersCount
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.displayMode
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.source.local.isLocal
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

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
internal fun ChapterRow(
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
internal fun chapterDateText(manga: Manga, dateUpload: Long): String {
    return if (manga.isEhBasedManga()) {
        MetadataUtil.EX_DATE_FORMAT
            .format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(dateUpload), ZoneId.systemDefault()))
    } else {
        relativeDateText(dateUpload)
    }
}

internal fun onChapterItemClick(
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
