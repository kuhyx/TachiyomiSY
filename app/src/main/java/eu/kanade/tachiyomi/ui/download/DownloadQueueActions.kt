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

// Moves one download to the top or bottom of its series' section and applies the new order.
internal fun DownloadQueueScreenModel.moveWithinSeries(item: DownloadItem, toTop: Boolean) {
    val headerItems = adapter?.headerItems ?: return
    val newDownloads = mutableListOf<Download>()
    headerItems.forEach { headerItem ->
        headerItem as DownloadHeaderItem
        if (headerItem == item.header) {
            headerItem.removeSubItem(item)
            if (toTop) headerItem.addSubItem(0, item) else headerItem.addSubItem(item)
        }
        newDownloads.addAll(headerItem.subItems.map { it.download })
    }
    reorder(newDownloads)
}

// Moves every download of the item's series before or after all the others.
internal fun DownloadQueueScreenModel.moveSeries(item: DownloadItem, toTop: Boolean) {
    val (selectedSeries, otherSeries) = queuedDownloads().partition { item.download.manga.id == it.manga.id }
    reorder(if (toTop) selectedSeries + otherSeries else otherSeries + selectedSeries)
}

internal fun DownloadQueueScreenModel.cancelSeries(item: DownloadItem) {
    val allDownloadsForSeries = queuedDownloads().filter { item.download.manga.id == it.manga.id }
    if (allDownloadsForSeries.isNotEmpty()) {
        cancel(allDownloadsForSeries)
    }
}

private fun DownloadQueueScreenModel.queuedDownloads(): List<Download> =
    adapter?.currentItems?.filterIsInstance<DownloadItem>()?.map(DownloadItem::download).orEmpty()
