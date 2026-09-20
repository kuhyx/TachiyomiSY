package eu.kanade.tachiyomi.ui.updates

import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.presentation.manga.components.ChapterDownloadAction
import eu.kanade.tachiyomi.data.download.model.Download
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tachiyomi.core.common.util.lang.launchNonCancellable
import uy.kohesive.injekt.api.get

// Update status of chapters.
// @param download download object containing progress.
internal fun UpdatesScreenModel.updateDownloadState(download: Download) {
    updateState { state ->
        val newItems = state.items.toMutableList().also { list ->
            val modifiedIndex = list.indexOfFirst { it.update.chapterId == download.chapter.id }
            if (modifiedIndex < 0) return@also

            val item = list[modifiedIndex]
            list[modifiedIndex] = item.copy(
                downloadStateProvider = { download.status },
                downloadProgressProvider = { download.progress },
            )
        }
        state.copy(items = newItems)
    }
}

internal fun UpdatesScreenModel.downloadChapters(items: List<UpdatesItem>, action: ChapterDownloadAction) {
    if (items.isEmpty()) return
    screenModelScope.launch {
        when (action) {
            ChapterDownloadAction.START -> {
                downloadChapters(items)
                if (items.any { it.downloadStateProvider() == Download.State.ERROR }) {
                    downloadManager.startDownloads()
                }
            }
            ChapterDownloadAction.START_NOW -> {
                val chapterId = items.singleOrNull()?.update?.chapterId ?: return@launch
                startDownloadingNow(chapterId)
            }
            ChapterDownloadAction.CANCEL -> {
                val chapterId = items.singleOrNull()?.update?.chapterId ?: return@launch
                cancelDownload(chapterId)
            }
            ChapterDownloadAction.DELETE -> {
                deleteChapters(items)
            }
        }
        toggleAllSelection(false)
    }
}

internal fun UpdatesScreenModel.startDownloadingNow(chapterId: Long) {
    downloadManager.startDownloadNow(chapterId)
}

internal fun UpdatesScreenModel.cancelDownload(chapterId: Long) {
    val activeDownload = downloadManager.getQueuedDownloadOrNull(chapterId) ?: return
    downloadManager.cancelQueuedDownloads(listOf(activeDownload))
    updateDownloadState(activeDownload.apply { transition(Download.State.NOT_DOWNLOADED) })
}

// Downloads the given list of chapters with the manager.
// @param updatesItem the list of chapters to download.
internal fun UpdatesScreenModel.downloadChapters(updatesItem: List<UpdatesItem>) {
    screenModelScope.launchNonCancellable {
        val groupedUpdates = updatesItem.groupBy { it.update.mangaId }.values
        for (updates in groupedUpdates) {
            val mangaId = updates.first().update.mangaId
            // Don't download if the manga or its source isn't available
            val manga = getManga.await(mangaId)?.takeIf { sourceManager.get(it.source) != null } ?: continue
            val chapters = updates.mapNotNull { getChapter.await(it.update.chapterId) }
            downloadManager.downloadChapters(manga, chapters)
        }
    }
}
