@file:OptIn(DelicateCoroutinesApi::class)

package eu.kanade.tachiyomi.data.download

import eu.kanade.tachiyomi.data.download.Downloader.Companion.CHAPTERS_PER_SOURCE_QUEUE_WARNING_THRESHOLD
import eu.kanade.tachiyomi.data.download.Downloader.Companion.DOWNLOADS_QUEUED_WARNING_THRESHOLD
import eu.kanade.tachiyomi.data.download.Downloader.Companion.TMP_DIR_SUFFIX
import eu.kanade.tachiyomi.data.download.Downloader.Companion.WARNING_NOTIF_TIMEOUT_MS
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.data.library.LibraryUpdateNotifier
import eu.kanade.tachiyomi.data.notification.NotificationHandler
import eu.kanade.tachiyomi.source.UnmeteredSource
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import logcat.LogPriority
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import uy.kohesive.injekt.api.get

// Prepares the subscriptions to start downloading.
internal fun Downloader.launchDownloaderJob() {
    if (isRunning) return

    downloaderJob = scope.launch {
        val activeDownloadsFlow = combine(
            queueState,
            downloadPreferences.parallelSourceLimit.changes(),
        ) { a, b -> a to b }.transformLatest { (queue, parallelCount) ->
            while (true) {
                val activeDownloads = queue.asSequence()
                    // Ignore completed downloads, leave them in the queue
                    .filter { it.status.value <= Download.State.DOWNLOADING.value }
                    .groupBy { it.source }
                    .toList()
                    .take(parallelCount)
                    .map { (_, downloads) -> downloads.first() }
                emit(activeDownloads)

                if (activeDownloads.isEmpty()) break
                // Suspend until a download enters the ERROR state
                val activeDownloadsErroredFlow =
                    combine(activeDownloads.map(Download::statusFlow)) { states ->
                        states.contains(Download.State.ERROR)
                    }.filter { it }
                activeDownloadsErroredFlow.first()
            }
        }
            .distinctUntilChanged()

        // Use supervisorScope to cancel child jobs when the downloader job is cancelled
        supervisorScope {
            val downloadJobs = mutableMapOf<Download, Job>()

            activeDownloadsFlow.collectLatest { activeDownloads ->
                val downloadJobsToStop = downloadJobs.filter { it.key !in activeDownloads }
                downloadJobsToStop.forEach { (download, job) ->
                    job.cancel()
                    downloadJobs.remove(download)
                }

                val downloadsToStart = activeDownloads.filter { it !in downloadJobs }
                downloadsToStart.forEach { download ->
                    downloadJobs[download] = launchDownloadJob(download)
                }
            }
        }
    }
}

/**
 * Creates a download object for every chapter and adds them to the downloads queue.
 *
 * @param manga the manga of the chapters to download.
 * @param chapters the list of chapters to download.
 * @param autoStart whether to start the downloader after enqueing the chapters.
 */
internal fun Downloader.queueChapters(manga: Manga, chapters: List<Chapter>, autoStart: Boolean) {
    if (chapters.isEmpty()) return

    val source = sourceManager.get(manga.source) as? HttpSource ?: return
    val wasEmpty = queueState.value.isEmpty()
    val chaptersToQueue = chapters.asSequence()
        // Filter out those already downloaded.
        .filter {
            provider.findChapterDir(
                it.name,
                it.scanlator,
                it.url,
                /* SY --> */ manga.ogTitle, /* SY <-- */
                source,
            ) == null
        }
        // Add chapters to queue from the start.
        .sortedByDescending { it.sourceOrder }
        // Filter out those already enqueued.
        .filter { chapter -> queueState.value.none { it.chapter.id == chapter.id } }
        // Create a download for each one.
        .map { Download(source, manga, it) }
        .toList()

    if (chaptersToQueue.isNotEmpty()) {
        addAllToQueue(chaptersToQueue)

        // Start downloader if needed
        if (autoStart && wasEmpty) {
            val queuedDownloads = queueState.value.count { it.source !is UnmeteredSource }
            val maxDownloadsFromSource = queueState.value
                .filter { it.source !is UnmeteredSource }
                .groupingBy { it.source }
                .eachCount()
                .values
                .maxOrNull()
                ?: 0
            if (
                queuedDownloads > DOWNLOADS_QUEUED_WARNING_THRESHOLD ||
                maxDownloadsFromSource > CHAPTERS_PER_SOURCE_QUEUE_WARNING_THRESHOLD
            ) {
                notifier.onWarning(
                    context.stringResource(
                        MR.strings.download_queue_size_warning,
                        context.stringResource(MR.strings.app_name),
                    ),
                    WARNING_NOTIF_TIMEOUT_MS,
                    NotificationHandler.openUrl(context, LibraryUpdateNotifier.HELP_WARNING_URL),
                )
            }
            DownloadJob.start(context)
        }
    }
}

// Downloads a chapter.
// @param download the chapter to be downloaded.
internal suspend fun Downloader.downloadChapter(download: Download) {
    val mangaDir = mangaDirWithSpace(download) ?: return
    val chapterDirname = provider.getChapterDirName(
        download.chapter.name,
        download.chapter.scanlator,
        download.chapter.url,
    )
    val tmpDir = mangaDir.createDirectory(chapterDirname + TMP_DIR_SUFFIX)!!

    try {
        // If the page list already exists, start from the file; otherwise pull it from the network.
        val pageList = download.pages ?: fetchPageList(download)
        download.transition(Download.State.DOWNLOADING)
        // Start downloading images, consider we can have downloaded images already
        downloadPages(download, pageList, tmpDir)
        // Do after download completes
        if (!isDownloadSuccessful(download, tmpDir)) {
            download.transition(Download.State.ERROR)
            return
        }
        finishChapter(download, mangaDir, chapterDirname, tmpDir)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (expected: Throwable) {
        // Logged whatever the cause; the caller carries on.
        // If the page list threw, it will resume here
        logcat(LogPriority.ERROR, expected)
        download.transition(Download.State.ERROR)
        notifier.onError(expected.message, download.chapter.name, download.manga.title, download.manga.id)
    }
}
