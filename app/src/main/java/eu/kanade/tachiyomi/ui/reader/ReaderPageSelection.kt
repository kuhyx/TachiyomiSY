package eu.kanade.tachiyomi.ui.reader

import androidx.lifecycle.viewModelScope
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.data.database.models.toDomainChapter
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel.Event
import eu.kanade.tachiyomi.ui.reader.chapter.ReaderChapterItem
import eu.kanade.tachiyomi.ui.reader.model.InsertPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.viewer.Viewer
import kotlinx.coroutines.flow.map
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.api.get

// Download-ahead starts once a quarter of the chapter has been read.
private const val DOWNLOAD_AHEAD_THRESHOLD = 0.25

// SY -->
internal fun ReaderViewModel.getChapters(): List<ReaderChapterItem> {
    val currentChapter = getCurrentChapter()

    return chapterList.map {
        ReaderChapterItem(
            chapter = it.chapter.toDomainChapter()!!,
            manga = manga!!,
            isCurrent = it.chapter.id == currentChapter?.chapter?.id,
            dateFormat = UiPreferences.dateFormat(uiPreferences.dateFormat.get()),
        )
    }
}

internal fun ReaderViewModel.onViewerLoaded(viewer: Viewer?) {
    updateState {
        it.copy(viewer = viewer)
    }
}

/**
 * Called every time a page changes on the reader. Used to mark the flag of chapters being
 * read, update tracking services, enqueue downloaded chapter deletion, and updating the active chapter if this
 * [page]'s chapter is different from the currently active.
 */
internal fun ReaderViewModel.onPageSelected(
    page: ReaderPage,
    currentPageText: String /* SY --> */,
    hasExtraPage: Boolean /* SY <-- */,
) {
    // InsertPage doesn't change page progress
    if (page is InsertPage) {
        return
    }

    // SY -->
    updateState { it.copy(currentPageText = currentPageText) }
    // SY <--

    val selectedChapter = page.chapter
    val pages = selectedChapter.pages ?: return

    // Save last page read and mark as read if needed
    viewModelScope.launchNonCancellable {
        progress.updateChapterProgress(selectedChapter, page/* SY --> */, hasExtraPage/* SY <-- */)
    }

    if (selectedChapter != getCurrentChapter()) {
        logcat { "Setting ${selectedChapter.chapter.url} as active" }
        loadNewChapter(selectedChapter)
    }

    val inDownloadRange = page.number.toDouble() / pages.size > DOWNLOAD_AHEAD_THRESHOLD
    if (inDownloadRange) {
        chapterDownloads.downloadNextChapters()
    }

    eventChannel.trySend(Event.PageChanged)
}

/**
 * Called when the user pressed the back button and is going to leave the reader. Used to
 * trigger deletion of the downloaded chapters.
 */
internal fun ReaderViewModel.onActivityFinish() {
    chapterDownloads.deletePendingChapters()
}

// Returns the currently active chapter.
internal fun ReaderViewModel.getCurrentChapter(): ReaderChapter? = state.value.currentChapter
