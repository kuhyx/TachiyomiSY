package eu.kanade.tachiyomi.data.download

import eu.kanade.tachiyomi.data.download.model.Download
import kotlinx.coroutines.flow.filter
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga
import uy.kohesive.injekt.api.get

// Returns true if all the queued downloads are in DOWNLOADED or ERROR state.
internal fun Downloader.areAllDownloadsFinished(): Boolean =
    queueState.value.none { it.status.value <= Download.State.DOWNLOADING.value }

internal fun Downloader.addAllToQueue(downloads: List<Download>) {
    updateQueueState {
        downloads.forEach { download ->
            download.transition(Download.State.QUEUE)
        }
        store.addAll(downloads)
        it + downloads
    }
}

internal fun Downloader.removeFromQueue(download: Download) {
    updateQueueState {
        store.remove(download)
        if (download.status == Download.State.DOWNLOADING || download.status == Download.State.QUEUE) {
            download.transition(Download.State.NOT_DOWNLOADED)
        }
        it - download
    }
}

internal inline fun Downloader.removeFromQueueIf(crossinline predicate: (Download) -> Boolean) {
    updateQueueState { queue ->
        val downloads = queue.filter { predicate(it) }
        store.removeAll(downloads)
        downloads.forEach { download ->
            if (download.status == Download.State.DOWNLOADING || download.status == Download.State.QUEUE) {
                download.transition(Download.State.NOT_DOWNLOADED)
            }
        }
        queue - downloads
    }
}

internal fun Downloader.removeFromQueue(chapters: List<Chapter>) {
    val chapterIds = chapters.map { it.id }
    removeFromQueueIf { it.chapter.id in chapterIds }
}

internal fun Downloader.removeFromQueue(manga: Manga) {
    removeFromQueueIf { it.manga.id == manga.id }
}

internal fun Downloader.internalClearQueue() {
    updateQueueState {
        it.forEach { download ->
            if (download.status == Download.State.DOWNLOADING || download.status == Download.State.QUEUE) {
                download.transition(Download.State.NOT_DOWNLOADED)
            }
        }
        store.clear()
        emptyList()
    }
}

internal fun Downloader.updateQueue(downloads: List<Download>) {
    val wasRunning = isRunning

    if (downloads.isEmpty()) {
        clearQueue()
        stop()
        return
    }

    pause()
    internalClearQueue()
    addAllToQueue(downloads)

    if (wasRunning) {
        start()
    }
}
