package eu.kanade.tachiyomi.data.download

import android.content.Context
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.data.cache.ChapterCache
import eu.kanade.tachiyomi.data.download.model.Download
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import logcat.LogPriority
import nl.adaptivity.xmlutil.serialization.XML
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.launchNow
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.track.interactor.GetTracks
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * This class is the one in charge of downloading chapters.
 *
 * Its queue contains the list of chapters to download.
 */
@OptIn(DelicateCoroutinesApi::class)
internal class Downloader(
    internal val context: Context,
    internal val provider: DownloadProvider,
    internal val cache: DownloadCache,
    internal val sourceManager: SourceManager = Injekt.get(),
    internal val chapterCache: ChapterCache = Injekt.get(),
    internal val downloadPreferences: DownloadPreferences = Injekt.get(),
    internal val xml: XML = Injekt.get(),
    internal val getCategories: GetCategories = Injekt.get(),
    internal val getTracks: GetTracks = Injekt.get(),
    // SY -->
    internal val sourcePreferences: SourcePreferences = Injekt.get(),
    // SY <--
) {

    // Store for persisting downloads across restarts.
    internal val store = DownloadStore(context)

    // Queue where active downloads are kept.
    private val _queueState = MutableStateFlow<List<Download>>(emptyList())
    val queueState = _queueState.asStateFlow()

    // Notifier for the downloader state and progress.
    internal val notifier by lazy { DownloadNotifier(context) }

    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    internal var downloaderJob: Job? = null

    /**
     * Whether the downloader is running.
     */
    val isRunning: Boolean
        get() = downloaderJob?.isActive ?: false

    /**
     * Whether the downloader is paused.
     */
    @Volatile
    var isPaused: Boolean = false

    init {
        launchNow {
            val chapters = async { store.restore() }
            addAllToQueue(chapters.await())
        }
    }

    /** Rewrites the queue; the extension files reach the private flow through it. */
    internal fun updateQueueState(func: (List<Download>) -> List<Download>) {
        _queueState.update(func)
    }

    /**
     * Starts the downloader. It doesn't do anything if it's already running or there isn't anything
     * to download.
     *
     * @return true if the downloader is started, false otherwise.
     */
    fun start(): Boolean {
        if (isRunning || queueState.value.isEmpty()) {
            return false
        }

        val pending = queueState.value.filter { it.status != Download.State.DOWNLOADED }
        pending.forEach { if (it.status != Download.State.QUEUE) it.transition(Download.State.QUEUE) }

        isPaused = false

        launchDownloaderJob()

        return pending.isNotEmpty()
    }

    /**
     * Stops the downloader.
     */
    fun stop(reason: String? = null) {
        cancelDownloaderJob()
        queueState.value
            .filter { it.status == Download.State.DOWNLOADING }
            .forEach { it.transition(Download.State.ERROR) }

        if (reason != null) {
            notifier.onWarning(reason)
            return
        }

        if (isPaused && queueState.value.isNotEmpty()) {
            notifier.onPaused()
        } else {
            notifier.onComplete()
        }

        isPaused = false

        DownloadJob.stop(context)
    }

    /**
     * Pauses the downloader.
     */
    fun pause() {
        cancelDownloaderJob()
        queueState.value
            .filter { it.status == Download.State.DOWNLOADING }
            .forEach { it.transition(Download.State.QUEUE) }
        isPaused = true
    }

    /**
     * Removes everything from the queue.
     */
    fun clearQueue() {
        cancelDownloaderJob()

        internalClearQueue()
        notifier.dismissProgress()
    }

    internal fun CoroutineScope.launchDownloadJob(download: Download) = launchIO {
        try {
            downloadChapter(download)

            // Remove successful download from queue
            if (download.status == Download.State.DOWNLOADED) {
                removeFromQueue(download)
            }
            if (areAllDownloadsFinished()) {
                stop()
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (expected: Throwable) {
            // Logged whatever the cause; the caller carries on.
            logcat(LogPriority.ERROR, expected)
            notifier.onError(expected.message)
            stop()
        }
    }

    // Destroys the downloader subscriptions.
    private fun cancelDownloaderJob() {
        downloaderJob?.cancel()
        downloaderJob = null
    }

    companion object {
        const val TMP_DIR_SUFFIX = "_tmp"
        const val WARNING_NOTIF_TIMEOUT_MS = 30_000L
        const val CHAPTERS_PER_SOURCE_QUEUE_WARNING_THRESHOLD = 15
        internal const val DOWNLOADS_QUEUED_WARNING_THRESHOLD = 30
    }
}

internal const val MIN_FILENAME_DIGITS = 3
internal const val PROGRESS_DONE = 100
internal const val DOWNLOAD_RETRIES = 3

// java.net.HttpURLConnection stops at 5xx; 416 says the resumed range is past the file's end.
internal const val HTTP_RANGE_NOT_SATISFIABLE = 416

internal fun inProgressFileName(filename: String) = "$filename.tmp"
