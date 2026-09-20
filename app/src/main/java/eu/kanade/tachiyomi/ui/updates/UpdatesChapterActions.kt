package eu.kanade.tachiyomi.ui.updates

import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.ui.updates.UpdatesScreenModel.Dialog
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.domain.chapter.model.ChapterUpdate
import uy.kohesive.injekt.api.get

/**
 * Mark the selected updates list as read/unread.
 * @param updates the list of selected updates.
 * @param read whether to mark chapters as read or unread.
 */
internal fun UpdatesScreenModel.markUpdatesRead(updates: List<UpdatesItem>, read: Boolean) {
    screenModelScope.launchIO {
        setReadStatus.await(
            read = read,
            chapters = updates
                .mapNotNull { getChapter.await(it.update.chapterId) }
                .toTypedArray(),
        )
    }
    toggleAllSelection(false)
}

/**
 * Bookmarks the given list of chapters.
 * @param updates the list of chapters to bookmark.
 * @param bookmark whether to bookmark or un-bookmark them.
 */
internal fun UpdatesScreenModel.bookmarkUpdates(updates: List<UpdatesItem>, bookmark: Boolean) {
    screenModelScope.launchIO {
        updates
            .filterNot { it.update.bookmark == bookmark }
            .map { ChapterUpdate(id = it.update.chapterId, bookmark = bookmark) }
            .let { updateChapter.awaitAll(it) }
    }
    toggleAllSelection(false)
}

/**
 * Delete selected chapters.
 *
 * @param updatesItem list of chapters
 */
internal fun UpdatesScreenModel.deleteChapters(updatesItem: List<UpdatesItem>) {
    screenModelScope.launchNonCancellable {
        updatesItem
            .groupBy { it.update.mangaId }
            .entries
            .forEach { (mangaId, updates) ->
                val manga = getManga.await(mangaId) ?: return@forEach
                val source = sourceManager.get(manga.source) ?: return@forEach
                val chapters = updates.mapNotNull { getChapter.await(it.update.chapterId) }
                downloadManager.deleteChapters(chapters, manga, source)
            }
    }
    toggleAllSelection(false)
}

internal fun UpdatesScreenModel.showConfirmDeleteChapters(updatesItem: List<UpdatesItem>) {
    setDialog(Dialog.DeleteConfirmation(updatesItem))
}
