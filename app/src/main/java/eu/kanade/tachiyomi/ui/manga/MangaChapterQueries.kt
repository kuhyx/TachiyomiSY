package eu.kanade.tachiyomi.ui.manga

import androidx.compose.runtime.getValue
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.manga.MangaScreenModel.State
import eu.kanade.tachiyomi.util.chapter.getNextUnread
import exh.source.isEhBasedManga
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.service.getChapterSort
import tachiyomi.domain.manga.model.sortDescending
import uy.kohesive.injekt.api.get

/**
 * Returns the next unread chapter or null if everything is read.
 */
internal fun MangaScreenModel.getNextUnreadChapter(): Chapter? {
    val successState = successState ?: return null
    return successState.chapters.getNextUnread(successState.manga)
}

internal fun MangaScreenModel.getUnreadChapters(): List<Chapter> {
    val chapterItems = if (skipFiltered) filteredChapters.orEmpty() else allChapters.orEmpty()
    return chapterItems
        .filter { (chapter, dlStatus) -> !chapter.read && dlStatus == Download.State.NOT_DOWNLOADED }
        .map { it.chapter }
}

internal fun MangaScreenModel.getUnreadChaptersSorted(): List<Chapter> {
    val manga = successState?.manga ?: return emptyList()
    val chaptersSorted = getUnreadChapters().sortedWith(getChapterSort(manga))
        // SY -->
        .let {
            if (manga.isEhBasedManga()) it.reversed() else it
        }
    // SY <--
    return if (manga.sortDescending()) chaptersSorted.reversed() else chaptersSorted
}

internal fun MangaScreenModel.getBookmarkedChapters(): List<Chapter> {
    val chapterItems = if (skipFiltered) filteredChapters.orEmpty() else allChapters.orEmpty()
    return chapterItems
        .filter { (chapter, dlStatus) -> chapter.bookmark && dlStatus == Download.State.NOT_DOWNLOADED }
        .map { it.chapter }
}
