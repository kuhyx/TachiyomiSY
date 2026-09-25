package eu.kanade.tachiyomi.ui.reader

import androidx.lifecycle.viewModelScope
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel.Event
import eu.kanade.tachiyomi.ui.reader.loader.ChapterLoader
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.model.ref
import eu.kanade.tachiyomi.ui.reader.model.unref
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.update
import logcat.LogPriority
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.source.local.isLocal
import uy.kohesive.injekt.api.get

// Loads the given [chapter] with this [loader] and updates the currently active chapters.
// Callers must handle errors.
internal suspend fun ReaderViewModel.loadChapter(
    loader: ChapterLoader,
    chapter: ReaderChapter,
    // SY -->
    page: Int? = null,
    // SY <--
): ViewerChapters {
    loader.loadChapter(chapter /* SY --> */, page/* SY <-- */)

    val chapterPos = chapterList.indexOf(chapter)
    val newChapters = ViewerChapters(
        chapter,
        chapterList.getOrNull(chapterPos - 1),
        chapterList.getOrNull(chapterPos + 1),
    )

    withUIContext {
        mutableState.update {
            // Add new references first to avoid unnecessary recycling
            newChapters.ref()
            it.viewerChapters?.unref()

            chapterToDownload = chapterDownloads.cancelQueuedDownloads(newChapters.currChapter)
            it.copy(
                viewerChapters = newChapters,
                bookmarked = newChapters.currChapter.chapter.bookmark,
            )
        }
    }
    return newChapters
}

// Called when the user changed to the given [chapter] when changing pages from the viewer.
// It's used only to set this chapter as active.
internal fun ReaderViewModel.loadNewChapter(chapter: ReaderChapter) {
    val loader = loader ?: return

    viewModelScope.launchIO {
        logcat { "Loading ${chapter.chapter.url}" }

        progress.updateHistory()
        progress.restartReadTimer()

        try {
            loadChapter(loader, chapter)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (expected: Throwable) {
            // Logged whatever the cause; the caller carries on.
            logcat(LogPriority.ERROR, expected)
        }
    }
}

internal fun ReaderViewModel.loadNewChapterFromDialog(chapter: Chapter) {
    viewModelScope.launchIO {
        // Reader chapters come from the database, so every id is set.
        val newChapter = chapterList.firstOrNull { it.chapter.id!! == chapter.id }
        if (newChapter != null) {
            loadAdjacent(newChapter)
        }
    }
}

// Called when the user is going to load the prev/next chapter through the toolbar buttons.
internal suspend fun ReaderViewModel.loadAdjacent(chapter: ReaderChapter) {
    val loader = loader ?: return

    logcat { "Loading adjacent ${chapter.chapter.url}" }

    mutableState.update { it.copy(isLoadingAdjacentChapter = true) }
    try {
        withIOContext {
            loadChapter(loader, chapter)
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (expected: Throwable) {
        // Logged whatever the cause; the caller carries on.
        logcat(LogPriority.ERROR, expected)
    } finally {
        mutableState.update { it.copy(isLoadingAdjacentChapter = false) }
    }
}

/**
 * Called when the viewers decide it's a good time to preload a [chapter] and improve the UX so
 * that the user doesn't have to wait too long to continue reading.
 */
internal suspend fun ReaderViewModel.preload(chapter: ReaderChapter) {
    if (chapter.state is ReaderChapter.State.Loaded || chapter.state == ReaderChapter.State.Loading) {
        return
    }

    if (chapter.pageLoader?.isLocal == false) {
        val manga = manga ?: return
        val dbChapter = chapter.chapter
        val isDownloaded = downloadManager.isChapterDownloaded(
            dbChapter.name,
            dbChapter.scanlator,
            dbChapter.url,
            /* SY --> */ manga.ogTitle /* SY <-- */,
            manga.source,
            skipCache = true,
        )
        if (isDownloaded) {
            chapter.state = ReaderChapter.State.Wait
        }
    }

    // Past the first check the chapter is waiting or failed, both of which (re)load.
    val loader = loader ?: return
    try {
        logcat { "Preloading ${chapter.chapter.url}" }
        loader.loadChapter(chapter)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (expected: Throwable) {
        // Rethrown (or wrapped) whatever the cause.
        return
    }
    eventChannel.trySend(Event.ReloadViewerChapters)
}

/**
 * Called from the activity to load and set the next chapter as active.
 */
internal suspend fun ReaderViewModel.loadNextChapter() {
    val nextChapter = state.value.viewerChapters?.nextChapter ?: return
    loadAdjacent(nextChapter)
}

/**
 * Called from the activity to load and set the previous chapter as active.
 */
internal suspend fun ReaderViewModel.loadPreviousChapter() {
    val prevChapter = state.value.viewerChapters?.prevChapter ?: return
    loadAdjacent(prevChapter)
}
