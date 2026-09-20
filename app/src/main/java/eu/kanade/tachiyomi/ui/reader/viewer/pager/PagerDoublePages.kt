package eu.kanade.tachiyomi.ui.reader.viewer.pager

import eu.kanade.tachiyomi.ui.reader.model.ChapterTransition
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderItem
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage

// SY -->
// How the pager pairs pages up in double-page mode. Pure list surgery; [PagerViewerAdapter.setJoinedItems]
// owns the adapter state around it.

/** Pairs [subItems] two to a spread, keeping full pages and the shifted page alone and each transition on its own. */
internal fun joinDoublePages(
    subItems: List<ReaderItem>,
    pageToShift: ReaderPage?,
    shiftDoublePage: Boolean,
): MutableList<Pair<ReaderItem, ReaderItem?>> {
    // Step 1: segment the pages and transition pages
    val (pagedItems, otherItems) = segmentByChapter(subItems)
    val subJoinedItems = mutableListOf<Pair<ReaderItem, ReaderItem?>>()

    // Step 2: run through each set of pages
    pagedItems.forEach { items ->
        items.forEach { it?.shiftedPage = false }
        // Step 3: If pages have been shifted,
        if (shiftDoublePage) {
            applyShift(items, pageToShift)
        }
        // Step 4: Add blanks for chunking
        insertIsolationBlanks(items)
        // Step 5: chunk em
        if (items.isNotEmpty()) {
            subJoinedItems.addAll(items.chunked(2).map { Pair(it.first()!!, it.getOrNull(1)) })
        }
        otherItems.getOrNull(pagedItems.indexOf(items))?.let {
            subJoinedItems.add(Pair(it, null))
        }
    }
    return subJoinedItems
}

// Splits the flat item list into per-chapter page runs, with the transitions that separate them.
private fun segmentByChapter(subItems: List<ReaderItem>): Pair<List<MutableList<ReaderPage?>>, List<ReaderItem>> {
    val pagedItems = mutableListOf<MutableList<ReaderPage?>>()
    val otherItems = mutableListOf<ReaderItem>()
    pagedItems.add(mutableListOf())
    subItems.forEach { readerItem ->
        when (readerItem) {
            is ReaderPage -> {
                if (pagedItems.last().isNotEmpty() &&
                    pagedItems.last().last()?.chapter?.chapter?.id != readerItem.chapter.chapter.id
                ) {
                    pagedItems.add(mutableListOf())
                }
                pagedItems.last().add(readerItem)
            }
            is ChapterTransition -> {
                otherItems.add(readerItem)
                pagedItems.add(mutableListOf())
            }
        }
    }
    return pagedItems to otherItems
}

private fun applyShift(items: MutableList<ReaderPage?>, pageToShift: ReaderPage?) {
    val index = items.indexOf(pageToShift)
    // Go from the current page and work your way back to the first page,
    // or the first page that's a full page.
    // This is done in case user tries to shift a page after a full page
    val fullPageBeforeIndex = if (index > -1) {
        items.take(index).indexOfLast { it?.fullPage == true }
    } else {
        -1
    }.coerceAtLeast(0)
    // Add a shifted page to the first place there isnt a full page
    for (i in fullPageBeforeIndex until items.size) {
        if (items[i]?.fullPage == false) {
            items[i]?.shiftedPage = true
            break
        }
    }
}

// Adds a 'blank' page after each full page. It will be used when chunked to solo a page.
private fun insertIsolationBlanks(items: MutableList<ReaderPage?>) {
    var itemIndex = 0
    while (itemIndex < items.size) {
        val currentItem = items[itemIndex]
        currentItem?.isolatedPage = false
        if (currentItem?.fullPage == true || currentItem?.shiftedPage == true) {
            items.add(itemIndex + 1, null)
            val previousIsEvenPage = itemIndex > 0 && items[itemIndex - 1] != null && (itemIndex - 1) % 2 == 0
            if (currentItem.fullPage && previousIsEvenPage) {
                // If a page is a full page, check if the previous page needs to be isolated
                // we should check if it's an even or odd page, since even pages need shifting
                // For example if Page 1 is full, Page 0 needs to be isolated
                // No need to take account shifted pages, because null additions should
                // always have an odd index in the list
                items[itemIndex - 1]?.isolatedPage = true
                items.add(itemIndex, null)
                itemIndex++
            }
            itemIndex++
        }
        itemIndex++
    }
}

/**
 * The spread index [newPage] lands on after re-joining; a transition that is not joined to anything
 * resolves to the spread holding the first (or last) page of the chapter it leads to.
 */
internal fun List<Pair<ReaderItem, ReaderItem?>>.joinedIndexOf(newPage: ReaderItem?): Int {
    val isUnjoinedTransition = newPage is ChapterTransition && none { it.first == newPage || it.second == newPage }
    if (!isUnjoinedTransition) {
        return indexOfFirst { it.first == newPage || it.second == newPage }
    }
    val filteredPages = filter { it.first is ReaderPage && (it.first as ReaderPage).chapter == newPage.to }
    val page = if (newPage is ChapterTransition.Next) {
        filteredPages.minByOrNull { (it.first as ReaderPage).index }?.first
    } else {
        filteredPages.maxByOrNull { (it.first as ReaderPage).index }?.first
    }
    return indexOfFirst { it.first == page || it.second == page }
}

/**
 * The item to move back to after re-joining: the old current (or its second page when [useSecondPage]),
 * unless the current chapter changed underneath it, in which case that chapter's first page.
 */
internal fun pageToRestore(
    oldCurrent: Pair<ReaderItem, ReaderItem?>?,
    currentChapter: ReaderChapter?,
    subItems: List<ReaderItem>,
    useSecondPage: Boolean,
): ReaderItem? = when {
    oldCurrent?.first is ReaderPage &&
        (oldCurrent.first as ReaderPage).chapter != currentChapter &&
        (oldCurrent.second as? ChapterTransition)?.from != currentChapter ->
        subItems.find { it is ReaderPage && it.chapter == currentChapter }
    useSecondPage -> oldCurrent?.second ?: oldCurrent?.first
    else -> oldCurrent?.first
}
// SY <--
