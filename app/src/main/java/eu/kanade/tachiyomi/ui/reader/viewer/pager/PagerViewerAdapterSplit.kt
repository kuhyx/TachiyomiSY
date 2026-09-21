package eu.kanade.tachiyomi.ui.reader.viewer.pager

import eu.kanade.tachiyomi.ui.reader.model.InsertPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import kotlinx.coroutines.delay
import tachiyomi.core.common.util.lang.launchUI

private const val SPLIT_DELAY_MS = 100L

internal fun PagerViewerAdapter.onPageSplit(currentPage: Any?, newPage: InsertPage) {
    if (currentPage !is ReaderPage) return

    val currentIndex = joinedItems.indexOfFirst { it.first == currentPage }

    // Put aside preprocessed pages for next chapter so they don't get removed when changing chapter
    if (currentPage.chapter.chapter.id != currentChapter?.chapter?.id) {
        preprocessed[newPage.index] = newPage
        return
    }

    val placeAtIndex = when (viewer) {
        is L2RPagerViewer,
        is VerticalPagerViewer,
        -> currentIndex + 1
        else -> currentIndex
    }

    // It will enter a endless cycle of insert pages
    val nextToInsertPage =
        (viewer is R2LPagerViewer && placeAtIndex - 1 >= 0 && joinedItems[placeAtIndex - 1].first is InsertPage) ||
            joinedItems[placeAtIndex].first is InsertPage
    if (nextToInsertPage) return

    joinedItems.add(placeAtIndex, newPage to null)

    notifyDataSetChanged()
}

internal fun PagerViewerAdapter.cleanupPageSplit() {
    val insertPages = joinedItems.filter { it.first is InsertPage }
    joinedItems.removeAll(insertPages)
    notifyDataSetChanged()
}

internal fun PagerViewerAdapter.splitDoublePages(current: ReaderPage) {
    val oldCurrent = joinedItems.getOrNull(viewer.pager.currentItem)
    val oldSecondPage = oldCurrent?.second as? ReaderPage
    val oldFirstPage = oldCurrent?.first as? ReaderPage
    val oldPage = oldSecondPage ?: oldFirstPage

    setJoinedItems(oldSecondPage == current || current.index + 1 < (oldPage?.index ?: 0))

    // The listener may be removed when we split a page, so the ui may not have updated properly
    // This case usually happens when we load a new chapter and the first 2 pages need to split og
    viewer.scope.launchUI {
        delay(SPLIT_DELAY_MS)
        viewer.onPageChange(viewer.pager.currentItem)
    }
}
