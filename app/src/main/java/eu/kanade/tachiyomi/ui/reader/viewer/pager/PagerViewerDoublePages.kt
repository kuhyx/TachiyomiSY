package eu.kanade.tachiyomi.ui.reader.viewer.pager

import eu.kanade.tachiyomi.ui.reader.model.InsertPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters

internal fun PagerViewer.onPageSplit(currentPage: ReaderPage, newPage: InsertPage) {
    activity.runOnUiThread {
        // Need to insert on UI thread else images will go blank
        adapter.onPageSplit(currentPage, newPage)
    }
}

internal fun PagerViewer.cleanupPageSplit() {
    adapter.cleanupPageSplit()
}

// SY -->
internal fun PagerViewer.setChaptersDoubleShift(chapters: ViewerChapters) {
    setChaptersInternal(chapters)
}

internal fun PagerViewer.updateShifting(page: ReaderPage? = null) {
    adapter.pageToShift = page ?: adapter.joinedItems.getOrNull(pager.currentItem)?.first as? ReaderPage
}

internal fun PagerViewer.splitDoublePages(currentPage: ReaderPage) {
    adapter.splitDoublePages(currentPage)
}

internal fun PagerViewer.getShiftedPage(): ReaderPage? = adapter.pageToShift
