package eu.kanade.tachiyomi.ui.manga

import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.presentation.manga.DownloadAction
import eu.kanade.presentation.manga.components.ChapterDownloadAction
import eu.kanade.tachiyomi.data.download.cancelQueuedDownloads
import eu.kanade.tachiyomi.data.download.deleteChapters
import eu.kanade.tachiyomi.data.download.getQueuedDownloadOrNull
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.download.startDownloads
import eu.kanade.tachiyomi.source.online.all.MergedSource
import exh.source.isEhBasedManga
import logcat.LogPriority
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.model.Chapter
import uy.kohesive.injekt.api.get

internal fun MangaDownloads.runChapterDownloadActions(
    items: List<ChapterList.Item>,
    action: ChapterDownloadAction,
) {
    when (action) {
        ChapterDownloadAction.START -> {
            startDownload(items.map { it.chapter }, false)
            if (items.any { it.downloadState == Download.State.ERROR }) {
                downloadManager.startDownloads()
            }
        }
        ChapterDownloadAction.START_NOW -> {
            val chapter = items.singleOrNull()?.chapter ?: return
            startDownload(listOf(chapter), true)
        }
        ChapterDownloadAction.CANCEL -> {
            val chapterId = items.singleOrNull()?.id ?: return
            val activeDownload = downloadManager.getQueuedDownloadOrNull(chapterId) ?: return
            downloadManager.cancelQueuedDownloads(listOf(activeDownload))
            updateDownloadState(activeDownload.apply { transition(Download.State.NOT_DOWNLOADED) })
        }
        ChapterDownloadAction.DELETE -> {
            deleteChapters(items.map { it.chapter })
        }
    }
}

internal fun MangaDownloads.runDownloadAction(action: DownloadAction) {
    val chaptersToDownload = when (action) {
        DownloadAction.UNREAD_CHAPTERS -> model.getUnreadChapters()
        DownloadAction.BOOKMARKED_CHAPTERS -> model.getBookmarkedChapters()
        else -> model.getUnreadChaptersSorted().take(checkNotNull(action.nextChapters))
    }
    if (chaptersToDownload.isNotEmpty()) {
        startDownload(chaptersToDownload, false)
    }
}

// Downloads the given list of chapters with the manager.
// @param chapters the list of chapters to download.
internal fun MangaDownloads.downloadChapters(chapters: List<Chapter>) {
    val state = model.successState ?: return
    if (state.source is MergedSource) {
        chapters.groupBy { it.mangaId }.forEach { map ->
            val manga = state.mergedData?.manga?.get(map.key)
            if (manga != null) {
                downloadManager.downloadChapters(manga, map.value)
            }
        }
    } else {
        /* SY <-- */
        val manga = state.manga
        downloadManager.downloadChapters(manga, chapters)
    }
    model.toggleAllSelection(false)
}

/**
 * Deletes the given list of chapter.
 *
 * @param chapters the list of chapters to delete.
 */
internal fun MangaDownloads.deleteChapters(chapters: List<Chapter>) {
    model.screenModelScope.launchNonCancellable {
        try {
            model.successState?.let { state ->
                downloadManager.deleteChapters(
                    chapters,
                    state.manga,
                    state.source,
                )
            }
        } catch (expected: Throwable) {
            // Logged whatever the cause; the caller carries on.
            logcat(LogPriority.ERROR, expected)
        }
    }
}

internal fun MangaDownloads.downloadNewChapters(chapters: List<Chapter>) {
    model.screenModelScope.launchNonCancellable {
        val manga = model.successState?.manga
        if (manga != null) {
            val chaptersToDownload = filterChaptersForDownload.await(manga, chapters)

            if (chaptersToDownload.isNotEmpty() /* SY --> */ && !manga.isEhBasedManga() /* SY <-- */) {
                downloadChapters(chaptersToDownload)
            }
        }
    }
}
