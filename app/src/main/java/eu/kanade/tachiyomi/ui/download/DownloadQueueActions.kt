package eu.kanade.tachiyomi.ui.download

import eu.kanade.tachiyomi.data.download.model.Download
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
