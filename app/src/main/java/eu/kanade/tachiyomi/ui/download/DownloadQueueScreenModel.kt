package eu.kanade.tachiyomi.ui.download

import android.view.MenuItem
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.databinding.DownloadListBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

private const val STOP_TIMEOUT_MS = 5000L

internal class DownloadQueueScreenModel(
    internal val downloadManager: DownloadManager = Injekt.get(),
) : ScreenModel {

    private val _state = MutableStateFlow(emptyList<DownloadHeaderItem>())
    val state = _state.asStateFlow()

    var controllerBinding: DownloadListBinding? = null

    /**
     * Adapter containing the active downloads.
     */
    var adapter: DownloadAdapter? = null

    // Map of jobs for active downloads.
    internal val progressJobs = mutableMapOf<Download, Job>()

    val listener = object : DownloadAdapter.DownloadItemListener {
        /**
         * Called when an item is released from a drag.
         *
         * @param position The position of the released item.
         */
        override fun onItemReleased(position: Int) {
            val adapter = adapter ?: return
            val downloads = adapter.headerItems.flatMap { header ->
                adapter.getSectionItems(header).map { item ->
                    (item as DownloadItem).download
                }
            }
            reorder(downloads)
        }

        /**
         * Called when the menu item of a download is pressed.
         *
         * @param position The position of the item
         * @param menuItem The menu Item pressed
         */
        override fun onMenuItemClick(position: Int, menuItem: MenuItem) {
            val item = adapter?.getItem(position) as? DownloadItem ?: return
            when (menuItem.itemId) {
                R.id.move_to_top, R.id.move_to_bottom -> {
                    moveWithinSeries(item, toTop = menuItem.itemId == R.id.move_to_top)
                }
                R.id.move_to_top_series, R.id.move_to_bottom_series -> {
                    moveSeries(item, toTop = menuItem.itemId == R.id.move_to_top_series)
                }
                R.id.cancel_download -> {
                    cancel(listOf(item.download))
                }
                R.id.cancel_series -> {
                    cancelSeries(item)
                }
            }
        }
    }

    init {
        screenModelScope.launch {
            downloadManager.queueState
                .map { downloads ->
                    downloads
                        .groupBy { it.source }
                        .map { entry ->
                            DownloadHeaderItem(entry.key.id, entry.key.name, entry.value.size).apply {
                                addSubItems(0, entry.value.map { DownloadItem(it, this) })
                            }
                        }
                }
                .collect { newList -> _state.update { newList } }
        }
    }

    val isDownloaderRunning = downloadManager.isDownloaderRunning
        .stateIn(screenModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), false)

    override fun onDispose() {
        for (job in progressJobs.values) {
            job.cancel()
        }
        progressJobs.clear()
        adapter = null
    }

    fun getDownloadStatusFlow() = downloadManager.statusFlow()
    fun getDownloadProgressFlow() = downloadManager.progressFlow()

    fun <R : Comparable<R>> reorderQueue(selector: (DownloadItem) -> R, reverse: Boolean = false) {
        val adapter = adapter ?: return
        val newDownloads = mutableListOf<Download>()
        adapter.headerItems.forEach { headerItem ->
            headerItem as DownloadHeaderItem
            headerItem.subItems = headerItem.subItems.sortedBy(selector).toMutableList().apply {
                if (reverse) {
                    reverse()
                }
            }
            newDownloads.addAll(headerItem.subItems.map { it.download })
        }
        reorder(newDownloads)
    }
}
