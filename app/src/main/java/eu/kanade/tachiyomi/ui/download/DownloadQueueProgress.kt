package eu.kanade.tachiyomi.ui.download

import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.source.model.Page
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import uy.kohesive.injekt.api.get
import kotlin.time.Duration.Companion.milliseconds

/**
 * Called when the status of a download changes.
 *
 * @param download the download whose status has changed.
 */
internal fun DownloadQueueScreenModel.onStatusChange(download: Download) {
    when (download.status) {
        Download.State.DOWNLOADING -> {
            launchProgressJob(download)
            // Initial update of the downloaded pages
            onUpdateDownloadedPages(download)
        }
        Download.State.DOWNLOADED -> {
            cancelProgressJob(download)
            onUpdateProgress(download)
            onUpdateDownloadedPages(download)
        }
        Download.State.ERROR -> {
            cancelProgressJob(download)
        }
        else -> {
            /* unused */
        }
    }
}

// Observe the progress of a download and notify the view.
// @param download the download to observe its progress.
internal fun DownloadQueueScreenModel.launchProgressJob(download: Download) {
    val job = screenModelScope.launch {
        while (download.pages == null) {
            delay(50.milliseconds)
        }

        val progressFlows = download.pages!!.map(Page::progressFlow)
        combine(progressFlows, Array<Int>::sum)
            .distinctUntilChanged()
            .debounce(50.milliseconds)
            .collectLatest {
                onUpdateProgress(download)
            }
    }

    // Avoid leaking jobs
    progressJobs.remove(download)?.cancel()

    progressJobs[download] = job
}

// Unsubscribes the given download from the progress subscriptions.
// @param download the download to unsubscribe.
internal fun DownloadQueueScreenModel.cancelProgressJob(download: Download) {
    progressJobs.remove(download)?.cancel()
}

// Called when the progress of a download changes.
// @param download the download whose progress has changed.
internal fun DownloadQueueScreenModel.onUpdateProgress(download: Download) {
    getHolder(download)?.notifyProgress()
}

/**
 * Called when a page of a download is downloaded.
 *
 * @param download the download whose page has been downloaded.
 */
internal fun DownloadQueueScreenModel.onUpdateDownloadedPages(download: Download) {
    getHolder(download)?.notifyDownloadedPages()
}

// Returns the holder for the given download.
// @param download the download to find.
// @return the holder of the download or null if it's not bound.
internal fun DownloadQueueScreenModel.getHolder(download: Download): DownloadHolder? =
    controllerBinding?.root?.findViewHolderForItemId(download.chapter.id) as? DownloadHolder
