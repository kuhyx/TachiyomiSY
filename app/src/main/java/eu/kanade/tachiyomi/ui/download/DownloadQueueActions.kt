package eu.kanade.tachiyomi.ui.download

import eu.kanade.tachiyomi.data.download.cancelQueuedDownloads
import eu.kanade.tachiyomi.data.download.clearQueue
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.download.pauseDownloads
import eu.kanade.tachiyomi.data.download.reorderQueue
import eu.kanade.tachiyomi.data.download.startDownloads
import uy.kohesive.injekt.api.get

internal fun DownloadQueueScreenModel.startDownloads() {
    downloadManager.startDownloads()
}

internal fun DownloadQueueScreenModel.pauseDownloads() {
    downloadManager.pauseDownloads()
}

internal fun DownloadQueueScreenModel.clearQueue() {
    downloadManager.clearQueue()
}

internal fun DownloadQueueScreenModel.reorder(downloads: List<Download>) {
    downloadManager.reorderQueue(downloads)
}

internal fun DownloadQueueScreenModel.cancel(downloads: List<Download>) {
    downloadManager.cancelQueuedDownloads(downloads)
}
