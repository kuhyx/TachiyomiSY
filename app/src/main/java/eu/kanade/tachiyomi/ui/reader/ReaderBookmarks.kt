package eu.kanade.tachiyomi.ui.reader

import androidx.lifecycle.viewModelScope
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.coroutines.flow.update
import logcat.LogPriority
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.model.ChapterUpdate
import uy.kohesive.injekt.api.get

internal fun ReaderViewModel.getSource() = manga?.source?.let { sourceManager.getOrStub(it) } as? HttpSource

internal fun ReaderViewModel.getChapterUrl(): String? {
    val sChapter = getCurrentChapter()?.chapter ?: return null
    val source = getSource() ?: return null

    return try {
        source.getChapterUrl(sChapter)
    } catch (expected: Exception) {
        // Logged whatever the cause; the caller carries on.
        logcat(LogPriority.ERROR, expected)
        null
    }
}

/**
 * Bookmarks the currently active chapter.
 */
internal fun ReaderViewModel.toggleChapterBookmark() {
    val chapter = getCurrentChapter()?.chapter ?: return
    val bookmarked = !chapter.bookmark
    chapter.bookmark = bookmarked

    viewModelScope.launchNonCancellable {
        updateChapter.await(
            ChapterUpdate(
                id = chapter.id!!,
                bookmark = bookmarked,
            ),
        )
    }

    mutableState.update {
        it.copy(
            bookmarked = bookmarked,
        )
    }
}

// SY -->
internal fun ReaderViewModel.toggleBookmark(chapterId: Long, bookmarked: Boolean) {
    val chapter = chapterList.find { it.chapter.id == chapterId }?.chapter ?: return
    chapter.bookmark = bookmarked
    viewModelScope.launchNonCancellable {
        updateChapter.await(
            ChapterUpdate(
                id = chapterId,
                bookmark = bookmarked,
            ),
        )
    }
}
