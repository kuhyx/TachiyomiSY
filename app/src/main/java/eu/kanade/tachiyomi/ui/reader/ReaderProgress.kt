package eu.kanade.tachiyomi.ui.reader

import android.app.Application
import androidx.lifecycle.viewModelScope
import eu.kanade.domain.sync.SyncPreferences
import eu.kanade.domain.track.interactor.TrackChapter
import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.tachiyomi.data.sync.SyncDataJob
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import exh.source.isEhBasedManga
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.domain.chapter.interactor.UpdateChapter
import tachiyomi.domain.chapter.model.ChapterUpdate
import tachiyomi.domain.history.interactor.UpsertHistory
import tachiyomi.domain.history.model.HistoryUpdate
import tachiyomi.domain.library.service.LibraryPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.Instant
import java.util.Date

/**
 * Reading progress bookkeeping: the page and chapter read state, history, the read timer, the
 * tracker update and the sync triggers that follow a completed chapter. Composed by
 * [ReaderViewModel].
 */
internal class ReaderProgress(
    private val model: ReaderViewModel,
    private val trackPreferences: TrackPreferences,
    private val libraryPreferences: LibraryPreferences,
    private val syncPreferences: SyncPreferences,
    private val upsertHistory: UpsertHistory = Injekt.get(),
    private val trackChapter: TrackChapter = Injekt.get(),
    private val updateChapter: UpdateChapter = Injekt.get(),
) {
    // The time the chapter was started reading.
    private var chapterReadStartTime: Long? = null

    // Saves the chapter progress (last read page and whether it's read)
    // if incognito mode isn't on.
    suspend fun updateChapterProgress(
        readerChapter: ReaderChapter,
        page: Page/* SY --> */,
        hasExtraPage: Boolean, /* SY <-- */
    ) {
        val pageIndex = page.index
        val syncTriggerOpt = syncPreferences.getSyncTriggerOptions()
        val isSyncEnabled = syncPreferences.isSyncEnabled()

        model.mutableState.update {
            it.copy(currentPage = pageIndex + 1)
        }
        readerChapter.requestedPage = pageIndex
        model.chapterPageIndex = pageIndex

        if (!model.incognitoMode && page.status !is Page.State.Error) {
            readerChapter.chapter.lastPageRead = pageIndex

            // SY -->
            if (
                readerChapter.pages?.lastIndex == pageIndex ||
                (hasExtraPage && readerChapter.pages?.lastIndex?.minus(1) == page.index)
            ) {
                // SY <--
                updateProgressOnComplete(readerChapter)

                // Check if syncing is enabled for chapter read:
                if (isSyncEnabled && syncTriggerOpt.syncOnChapterRead) {
                    SyncDataJob.startNow(Injekt.get<Application>())
                }
            }

            updateChapter.await(
                ChapterUpdate(
                    id = readerChapter.chapter.id!!,
                    read = readerChapter.chapter.read,
                    lastPageRead = readerChapter.chapter.lastPageRead.toLong(),
                ),
            )

            // SY -->
            // Check if syncing is enabled for chapter open:
            if (isSyncEnabled && syncTriggerOpt.syncOnChapterOpen && readerChapter.chapter.lastPageRead == 0) {
                SyncDataJob.startNow(Injekt.get<Application>())
            }
            // SY <--
        }
    }

    private suspend fun updateProgressOnComplete(readerChapter: ReaderChapter) {
        readerChapter.chapter.read = true
        // SY -->
        if (model.manga?.isEhBasedManga() == true) {
            model.viewModelScope.launchNonCancellable {
                val chapterUpdates = model.unfilteredChapterList
                    .filter { it.sourceOrder > readerChapter.chapter.sourceOrder }
                    .map { chapter ->
                        ChapterUpdate(
                            id = chapter.id,
                            read = true,
                        )
                    }
                updateChapter.awaitAll(chapterUpdates)
            }
        }
        // SY <--

        updateTrackChapterRead(readerChapter)
        model.chapterDownloads.deleteChapterIfNeeded(readerChapter)

        val markDuplicateAsRead = libraryPreferences.markDuplicateReadChapterAsRead.get()
            .contains(LibraryPreferences.MARK_DUPLICATE_CHAPTER_READ_EXISTING)
        if (!markDuplicateAsRead) return

        val duplicateUnreadChapters = model.unfilteredChapterList
            .mapNotNull { chapter ->
                if (
                    !chapter.read &&
                    chapter.isRecognizedNumber &&
                    chapter.chapterNumber.toFloat() == readerChapter.chapter.chapter_number
                ) {
                    ChapterUpdate(id = chapter.id, read = true)
                } else {
                    null
                }
            }
        updateChapter.awaitAll(duplicateUnreadChapters)
        // SY -->
        duplicateUnreadChapters.forEach { chapterUpdate ->
            val chapter = model.unfilteredChapterList.first { it.id == chapterUpdate.id }
            model.chapterDownloads.deleteChapterIfNeeded(ReaderChapter(chapter))
        }
        // SY <--
    }

    fun restartReadTimer() {
        chapterReadStartTime = Instant.now().toEpochMilli()
    }

    /**
     * Saves the chapter last read history if incognito mode isn't on.
     */
    suspend fun updateHistory() {
        model.getCurrentChapter()?.let { readerChapter ->
            if (model.incognitoMode) return@let

            val chapterId = readerChapter.chapter.id!!
            val endTime = Date()
            val sessionReadDuration = chapterReadStartTime?.let { endTime.time - it } ?: 0

            upsertHistory.await(HistoryUpdate(chapterId, endTime, sessionReadDuration))
            chapterReadStartTime = null
        }
    }

    // Starts the service that updates the last chapter read in sync services. This operation
    // will run in a background thread and errors are ignored.
    private fun updateTrackChapterRead(readerChapter: ReaderChapter) {
        if (model.incognitoMode) return
        if (!trackPreferences.autoUpdateTrack.get()) return

        val manga = model.manga ?: return
        val context = Injekt.get<Application>()

        model.viewModelScope.launchNonCancellable {
            trackChapter.await(context, manga.id, readerChapter.chapter.chapter_number.toDouble())
        }
    }
}
