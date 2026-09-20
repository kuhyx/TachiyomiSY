package eu.kanade.tachiyomi.data.library

import eu.kanade.tachiyomi.data.download.startDownloads
import eu.kanade.tachiyomi.util.storage.getUriCompat
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.preference.getAndSet
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.model.NoChaptersException
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.model.SourceNotInstalledException
import tachiyomi.i18n.MR
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/** The tallies of one chapter-list update pass, shared by the per-source coroutines. */
@OptIn(ExperimentalAtomicApi::class)
internal data class LibraryUpdateRun(val fetchWindow: Pair<Long, Long>) {
    val progressCount = AtomicInt(0)
    val currentlyUpdatingManga = CopyOnWriteArrayList<Manga>()
    val newUpdates = CopyOnWriteArrayList<Pair<Manga, Array<Chapter>>>()
    val failedUpdates = CopyOnWriteArrayList<Pair<Manga, String?>>()
    val hasDownloads = AtomicBoolean(false)
}

/**
 * Fetches one manga's chapters under the progress notification; a failure lands in
 * [LibraryUpdateRun.failedUpdates].
 */
@OptIn(ExperimentalAtomicApi::class)
internal suspend fun LibraryUpdateJob.updateIfInLibrary(manga: Manga, run: LibraryUpdateRun) {
    // Don't continue to update if manga is not in library
    if (getManga.await(manga.id)?.favorite != true) return
    withUpdateNotification(run.currentlyUpdatingManga, run.progressCount, manga) {
        try {
            val newChapters = updateManga(manga, run.fetchWindow).sortedByDescending { it.sourceOrder }
            if (newChapters.isNotEmpty()) {
                val chaptersToDownload = filterChaptersForDownload.await(manga, newChapters)
                if (chaptersToDownload.isNotEmpty()) {
                    downloadChapters(manga, chaptersToDownload)
                    run.hasDownloads.store(true)
                }
                libraryPreferences.newUpdatesCount.getAndSet { it + newChapters.size }
                // Convert to the manga that contains new chapters
                run.newUpdates.add(manga to newChapters.toTypedArray())
            }
        } catch (expected: Throwable) {
            // Any failure ends here and the fallback below applies.
            run.failedUpdates.add(manga to updateErrorMessage(expected))
        }
    }
}

private fun LibraryUpdateJob.updateErrorMessage(expected: Throwable): String? = when (expected) {
    is NoChaptersException -> applicationContext.stringResource(MR.strings.no_chapters_error)
    // failedUpdates already carries the source; the message needn't.
    is SourceNotInstalledException -> applicationContext.stringResource(MR.strings.loader_not_implemented_error)
    else -> expected.message
}

/** The end-of-run notifications: new chapters (and their downloads) and the failure report. */
@OptIn(ExperimentalAtomicApi::class)
internal fun LibraryUpdateJob.reportRun(run: LibraryUpdateRun) {
    notifier.cancelProgressNotification()
    if (run.newUpdates.isNotEmpty()) {
        notifier.showUpdateNotifications(run.newUpdates)
        if (run.hasDownloads.load()) {
            downloadManager.startDownloads()
        }
    }
    if (run.failedUpdates.isNotEmpty()) {
        val errorFile = writeErrorFile(run.failedUpdates)
        notifier.showUpdateErrorNotification(run.failedUpdates.size, errorFile.getUriCompat(applicationContext))
    }
}
