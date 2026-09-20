package eu.kanade.tachiyomi.ui.reader

import androidx.lifecycle.viewModelScope
import eu.kanade.tachiyomi.data.database.models.toDomainChapter
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.cancelQueuedDownloads
import eu.kanade.tachiyomi.data.download.deletePendingChapters
import eu.kanade.tachiyomi.data.download.enqueueChaptersToDelete
import eu.kanade.tachiyomi.data.download.getQueuedDownloadOrNull
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.reader.loader.DownloadPageLoader
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.util.chapter.removeDuplicates
import tachiyomi.core.common.storage.UniFileTempFileManager
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.history.interactor.GetNextChapters
import uy.kohesive.injekt.api.get

/**
 * The reader's download bookkeeping: downloading ahead while reading, cancelling a queued
 * download of the chapter being left, and deleting read chapters N slots behind. Composed by
 * [ReaderViewModel].
 */
internal class ReaderChapterDownloads(
    private val model: ReaderViewModel,
    private val readerPreferences: ReaderPreferences,
    private val downloadPreferences: DownloadPreferences,
    private val downloadManager: DownloadManager,
    private val tempFileManager: UniFileTempFileManager,
    private val getNextChapters: GetNextChapters,
) {
    private val downloadAheadAmount = downloadPreferences.autoDownloadWhileReading.get()

    fun downloadNextChapters() {
        if (downloadAheadAmount == 0) return
        val manga = model.manga ?: return

        // Only download ahead if current + next chapter is already downloaded too to avoid jank
        if (model.getCurrentChapter()?.pageLoader !is DownloadPageLoader) return
        val nextChapter = model.state.value.viewerChapters?.nextChapter?.chapter ?: return

        model.viewModelScope.launchIO {
            val isNextChapterDownloaded = downloadManager.isChapterDownloaded(
                nextChapter.name,
                nextChapter.scanlator,
                nextChapter.url,
                // SY -->
                manga.ogTitle,
                // SY <--
                manga.source,
            )
            if (isNextChapterDownloaded) {
                val chaptersToDownload = getNextChapters.await(manga.id, nextChapter.id!!).run {
                    if (readerPreferences.skipDupe.get()) {
                        removeDuplicates(nextChapter.toDomainChapter()!!)
                    } else {
                        this
                    }
                }.take(downloadAheadAmount)

                downloadManager.downloadChapters(
                    manga,
                    chaptersToDownload,
                )
            }
        }
    }

    // Removes [currentChapter] from download queue
    // if setting is enabled and [currentChapter] is queued for download.
    fun cancelQueuedDownloads(currentChapter: ReaderChapter): Download? {
        return downloadManager.getQueuedDownloadOrNull(currentChapter.chapter.id!!)?.also {
            downloadManager.cancelQueuedDownloads(listOf(it))
        }
    }

    // Determines if deleting option is enabled and nth to last chapter actually exists.
    // If both conditions are satisfied enqueues chapter for delete
    // @param currentChapter current chapter, which is going to be marked as read.
    fun deleteChapterIfNeeded(currentChapter: ReaderChapter) {
        val removeAfterReadSlots = downloadPreferences.removeAfterReadSlots.get()
        if (removeAfterReadSlots == -1) return

        // Determine which chapter should be deleted and enqueue
        val currentChapterPosition = model.chapterList.indexOf(currentChapter)
        val chapterToDelete = model.chapterList.getOrNull(currentChapterPosition - removeAfterReadSlots)

        // If chapter is completely read, no need to download it
        model.chapterToDownload = null

        if (chapterToDelete != null) {
            enqueueDeleteReadChapters(chapterToDelete)
        }
    }

    // Enqueues this [chapter] to be deleted when [deletePendingChapters] is called. The download
    // manager handles persisting it across process deaths.
    fun enqueueDeleteReadChapters(chapter: ReaderChapter) {
        if (!chapter.chapter.read) return
        val mergedManga = model.state.value.mergedManga
        // SY -->
        val manga = if (mergedManga.isNullOrEmpty()) {
            model.manga
        } else {
            mergedManga[chapter.chapter.mangaId]
        } ?: return
        // SY <--

        model.viewModelScope.launchNonCancellable {
            downloadManager.enqueueChaptersToDelete(listOf(chapter.chapter.toDomainChapter()!!), manga)
        }
    }

    // Deletes all the pending chapters. This operation will run in a background thread and errors
    // are ignored.
    fun deletePendingChapters() {
        model.viewModelScope.launchNonCancellable {
            downloadManager.deletePendingChapters()
            tempFileManager.deleteTempFiles()
        }
    }
}
