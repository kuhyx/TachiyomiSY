package eu.kanade.tachiyomi.data.download

import eu.kanade.tachiyomi.data.download.model.Download
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import uy.kohesive.injekt.api.get

// For use by DownloadService only
internal fun DownloadManager.downloaderStart() = downloader.start()

internal fun DownloadManager.downloaderStop(reason: String? = null) = downloader.stop(reason)

/**
 * Tells the downloader to begin downloads.
 */
internal fun DownloadManager.startDownloads() {
    if (downloader.isRunning) return

    if (DownloadJob.isRunning(context)) {
        downloader.start()
    } else {
        DownloadJob.start(context)
    }
}

/**
 * Tells the downloader to pause downloads.
 */
internal fun DownloadManager.pauseDownloads() {
    downloader.pause()
    downloader.stop()
}

/**
 * Empties the download queue.
 */
internal fun DownloadManager.clearQueue() {
    downloader.clearQueue()
    downloader.stop()
}

/**
 * Returns the download from queue if the chapter is queued for download
 * else it will return null which means that the chapter is not queued for download.
 *
 * @param chapterId the chapter to check.
 */
internal fun DownloadManager.getQueuedDownloadOrNull(
    chapterId: Long,
): Download? = queueState.value.find { it.chapter.id == chapterId }

internal fun DownloadManager.startDownloadNow(chapterId: Long) {
    val existingDownload = getQueuedDownloadOrNull(chapterId)
    // If not in queue try to start a new download
    val toAdd = existingDownload ?: runBlocking { Download.fromChapterId(chapterId) } ?: return
    queueState.value.toMutableList().apply {
        existingDownload?.let { remove(it) }
        add(0, toAdd)
        reorderQueue(this)
    }
    startDownloads()
}

/**
 * Reorders the download queue.
 *
 * @param downloads value to set the download queue to
 */
internal fun DownloadManager.reorderQueue(downloads: List<Download>) {
    downloader.updateQueue(downloads)
}

/**
 * Tells the downloader to enqueue the given list of downloads at the start of the queue.
 *
 * @param downloads the list of downloads to enqueue.
 */
internal fun DownloadManager.addDownloadsToStartOfQueue(downloads: List<Download>) {
    if (downloads.isEmpty()) return
    queueState.value.toMutableList().apply {
        addAll(0, downloads)
        reorderQueue(this)
    }
    if (!DownloadJob.isRunning(context)) startDownloads()
}

internal fun DownloadManager.cancelQueuedDownloads(downloads: List<Download>) {
    removeFromDownloadQueue(downloads.map { it.chapter })
}
