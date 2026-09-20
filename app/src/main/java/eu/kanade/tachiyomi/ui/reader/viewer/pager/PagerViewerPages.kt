package eu.kanade.tachiyomi.ui.reader.viewer.pager

import androidx.core.view.children
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.InsertPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.onPageSelected
import eu.kanade.tachiyomi.ui.reader.requestPreloadChapter
import eu.kanade.tachiyomi.ui.reader.showMenu
import tachiyomi.core.common.util.system.logcat

// Returns the PagerPageHolder for the provided page.
internal fun PagerViewer.getPageHolder(page: ReaderPage): PagerPageHolder? =
    pager.children
        .filterIsInstance<PagerPageHolder>()
        .firstOrNull { it.item.first == page || it.item.second == page }

/**
 * Called when a new page (either a [ReaderPage] or [ChapterTransition]) is marked as active.
 */
internal fun PagerViewer.onPageChange(position: Int) {
    val pagePair = adapter.joinedItems.getOrNull(position)
    val page = pagePair?.first
    if (page != null && currentPage != page) {
        val allowPreload = checkAllowPreload(page as? ReaderPage)
        val forward = when {
            currentPage is ReaderPage && page is ReaderPage -> {
                // if both pages have the same number, it's a split page with an InsertPage
                if (page.number == (currentPage as ReaderPage).number) {
                    // the InsertPage is always the second in the reading direction
                    page is InsertPage
                } else {
                    page.number > (currentPage as ReaderPage).number
                }
            }
            currentPage is ChapterTransition.Prev && page is ReaderPage -> {
                false
            }
            else -> {
                true
            }
        }
        currentPage = page
        when (page) {
            is ReaderPage -> onReaderPageSelected(page, allowPreload, forward, pagePair.second != null)
            is ChapterTransition -> onTransitionSelected(page)
        }
    }
}

internal fun PagerViewer.checkAllowPreload(page: ReaderPage?): Boolean {
    // Page is transition page - preload allowed
    page ?: return true

    // Initial opening - preload allowed
    currentPage ?: return true

    // Allow preload for
    // 1. Going to next chapter from chapter transition
    // 2. Going between pages of same chapter
    // 3. Next chapter page
    return when (page.chapter) {
        (currentPage as? ChapterTransition.Next)?.to -> true
        (currentPage as? ReaderPage)?.chapter -> true
        adapter.nextTransition?.to -> true
        else -> false
    }
}

// Called when a [ReaderPage] is marked as active. It notifies the
// activity of the change and requests the preload of the next chapter if this is the last page.
internal fun PagerViewer.onReaderPageSelected(
    page: ReaderPage,
    allowPreload: Boolean,
    forward: Boolean,
    hasExtraPage: Boolean,
) {
    val pages = page.chapter.pages ?: return
    logcat { "onReaderPageSelected: ${page.number}/${pages.size}" }
    activity.onPageSelected(page, hasExtraPage)

    // Notify holder of page change
    getPageHolder(page)?.onPageSelected(forward)

    // Skip preload on inserts it causes unwanted page jumping
    if (page is InsertPage) {
        return
    }

    // Preload next chapter once we're within the last 5 pages of the current chapter
    val inPreloadRange = pages.size - page.number < PRELOAD_PAGES_BEFORE_END
    if (inPreloadRange && allowPreload && page.chapter == adapter.currentChapter) {
        logcat { "Request preload next chapter because we're at page ${page.number} of ${pages.size}" }
        adapter.nextTransition?.to?.let(activity::requestPreloadChapter)
    }
}

// Called when a [ChapterTransition] is marked as active. It request the
// preload of the destination chapter of the transition.
internal fun PagerViewer.onTransitionSelected(transition: ChapterTransition) {
    logcat { "onTransitionSelected: $transition" }
    val toChapter = transition.to
    if (toChapter != null) {
        logcat { "Request preload destination chapter because we're on the transition" }
        activity.requestPreloadChapter(toChapter)
    } else if (transition is ChapterTransition.Next) {
        // No more chapters, show menu because the user is probably going to close the reader
        activity.showMenu()
    }
}
