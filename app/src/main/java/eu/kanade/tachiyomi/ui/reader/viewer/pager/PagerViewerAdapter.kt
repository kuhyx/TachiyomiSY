package eu.kanade.tachiyomi.ui.reader.viewer.pager

import android.view.View
import android.view.ViewGroup
import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.InsertPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderItem
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.viewer.calculateChapterGap
import eu.kanade.tachiyomi.util.system.createReaderThemeContext
import eu.kanade.tachiyomi.widget.ViewPagerAdapter
import kotlinx.coroutines.delay
import tachiyomi.core.common.util.lang.launchUI
import tachiyomi.core.common.util.system.logcat

// Pager adapter used by this [viewer] to where [ViewerChapters] updates are posted.
private const val SPLIT_DELAY_MS = 100L

internal class PagerViewerAdapter(private val viewer: PagerViewer) : ViewPagerAdapter() {

    /**
     * Paired list of currently set items.
     */
    var joinedItems: MutableList<Pair<ReaderItem, ReaderItem?>> = mutableListOf()
        private set

    // Single list of items.
    private var subItems: MutableList<ReaderItem> = mutableListOf()

    // Holds preprocessed items so they don't get removed when changing chapter.
    private var preprocessed: MutableMap<Int, InsertPage> = mutableMapOf()

    var nextTransition: ChapterTransition.Next? = null
        private set

    var currentChapter: ReaderChapter? = null

    // SY -->

    /** Page used to start the shifted pages. */
    var pageToShift: ReaderPage? = null

    // Varibles used to check if config of the pages have changed.
    private var shifted = viewer.config.shiftDoublePage
    private var doubledUp = viewer.config.doublePages
    // SY <--

    // Context that has been wrapped to use the correct theme values based on the
    // current app theme and reader background color.
    private var readerThemedContext = viewer.activity.createReaderThemeContext()

    /**
     * Updates this adapter with the given [chapters]. It handles setting a few pages of the
     * next/previous chapter to allow seamless transitions and inverting the pages if the viewer
     * has R2L direction.
     */
    fun setChapters(chapters: ViewerChapters, forceTransition: Boolean) {
        val newItems = mutableListOf<ReaderItem>()

        // Forces chapter transition if there is missing chapters
        val prevHasMissingChapters = calculateChapterGap(chapters.currChapter, chapters.prevChapter) > 0
        val nextHasMissingChapters = calculateChapterGap(chapters.nextChapter, chapters.currChapter) > 0

        // Add previous chapter pages and transition
        chapters.prevChapter?.pages?.let(newItems::addAll)

        // Skip transition page if the chapter is loaded & current page is not a transition page
        if (prevHasMissingChapters || forceTransition || chapters.prevChapter?.state !is ReaderChapter.State.Loaded) {
            newItems.add(ChapterTransition.Prev(chapters.currChapter, chapters.prevChapter))
        }

        var insertPageLastPage: InsertPage? = null

        // Add current chapter.
        val currPages = chapters.currChapter.pages
        if (currPages != null) {
            val pages = currPages.toMutableList()
            insertPageLastPage = insertPreprocessed(pages)
            newItems.addAll(pages)
        }

        currentChapter = chapters.currChapter

        // Add next chapter transition and pages.
        nextTransition = ChapterTransition.Next(chapters.currChapter, chapters.nextChapter)
            .also {
                if (
                    nextHasMissingChapters ||
                    forceTransition ||
                    chapters.nextChapter?.state !is ReaderChapter.State.Loaded
                ) {
                    newItems.add(it)
                }
            }

        chapters.nextChapter?.pages?.let(newItems::addAll)

        // Resets double-page splits, else insert pages get misplaced
        subItems.filterIsInstance<InsertPage>().also { subItems.removeAll(it) }

        preprocessed = mutableMapOf()
        subItems = newItems.toMutableList()

        setJoinedItems(updateDoublePageState())

        // Will skip insert page otherwise
        if (insertPageLastPage != null) {
            viewer.moveToPage(insertPageLastPage)
        }
    }

    // Insert preprocessed pages into current page list; the one after the last page needs a jump afterwards.
    private fun insertPreprocessed(pages: MutableList<ReaderPage>): InsertPage? {
        val lastPage = pages.last()
        var insertPageLastPage: InsertPage? = null
        preprocessed.keys.sortedDescending()
            .forEach { key ->
                if (lastPage.index == key) {
                    insertPageLastPage = preprocessed[key]
                }
                preprocessed[key]?.let { pages.add(key + 1, it) }
            }
        return insertPageLastPage
    }

    // Re-reads the shift and double-page settings; true when the pairing should start from the second page.
    private fun updateDoublePageState(): Boolean {
        var useSecondPage = false
        if (shifted != viewer.config.shiftDoublePage || (doubledUp != viewer.config.doublePages && doubledUp)) {
            if (shifted && doubledUp == viewer.config.doublePages) {
                useSecondPage = true
            }
            shifted = viewer.config.shiftDoublePage
        }
        doubledUp = viewer.config.doublePages
        return useSecondPage
    }

    /**
     * Returns the amount of items of the adapter.
     */
    override fun getCount(): Int = joinedItems.size

    /**
     * Creates a new view for the item at the given [position].
     */
    override fun createView(container: ViewGroup, position: Int): View {
        val item = joinedItems[position].first
        val item2 = joinedItems[position].second
        return when (item) {
            is ReaderPage -> PagerPageHolder(readerThemedContext, viewer, item, item2 as? ReaderPage)
            is ChapterTransition -> PagerTransitionHolder(readerThemedContext, viewer, item)
            // SY --> else -> throw NotImplementedError("Holder for ${item.javaClass} not implemented") SY <--
        }
    }

    /**
     * Returns the current position of the given [view] on the adapter.
     */
    override fun getItemPosition(view: Any): Int {
        if (view is PositionableView) {
            val position = joinedItems.indexOf(view.item)
            if (position != -1) {
                return position
            } else {
                logcat { "Position for ${view.item} not found" }
            }
        }
        return POSITION_NONE
    }

    fun onPageSplit(currentPage: Any?, newPage: InsertPage) {
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

    fun cleanupPageSplit() {
        val insertPages = joinedItems.filter { it.first is InsertPage }
        joinedItems.removeAll(insertPages)
        notifyDataSetChanged()
    }

    fun refresh() {
        readerThemedContext = viewer.activity.createReaderThemeContext()
    }

    // SY -->
    private fun setJoinedItems(useSecondPage: Boolean = false) {
        val oldCurrent = joinedItems.getOrNull(viewer.pager.currentItem)
        val joined = if (!viewer.config.doublePages) {
            // If not in double mode, set up items like before
            subItems.forEach { (it as? ReaderPage)?.shiftedPage = false }
            subItems.map { Pair<ReaderItem, ReaderItem?>(it, null) }.toMutableList()
        } else {
            joinDoublePages(subItems, pageToShift, viewer.config.shiftDoublePage)
        }
        if (viewer is R2LPagerViewer) {
            joined.reverse()
        }
        this.joinedItems = joined
        notifyDataSetChanged()

        // Step 6: Move back to our previous page or transition page
        // The listener is likely off around now, but either way when shifting or doubling,
        // we need to set the page back correctly
        // We will however shift to the first page of the new chapter if the last page we were are
        // on is not in the new chapter that has loaded
        // An adapter that had no current page keeps its position.
        if (oldCurrent == null && !useSecondPage) return
        val newPage = pageToRestore(oldCurrent, currentChapter, subItems, useSecondPage)
        viewer.pager.setCurrentItem(joinedItems.joinedIndexOf(newPage), false)
    }

    fun splitDoublePages(current: ReaderPage) {
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
    // SY <--
}
