package eu.kanade.tachiyomi.ui.manga

import eu.kanade.core.util.addOrRemove

/**
 * The chapter multi-select of the manga screen: which ids are selected and the first/last
 * selected index that long-press range selection extends from. Every operation returns the
 * chapter list with the `selected` flags updated; the caller stores it in the screen state.
 */
internal class ChapterSelection {
    private val selectedPositions: Array<Int> = arrayOf(-1, -1) // first and last selected index in list
    val selectedChapterIds: HashSet<Long> = HashSet()

    fun toggle(
        chapters: List<ChapterList.Item>,
        item: ChapterList.Item,
        selected: Boolean,
        fromLongPress: Boolean,
    ): List<ChapterList.Item> {
        return chapters.toMutableList().apply {
            val selectedIndex = chapters.indexOfFirst { it.id == item.chapter.id }
            if (!(selectedIndex < 0)) {
                val selectedItem = get(selectedIndex)
                if (selectedItem.selected != selected) {
                    val firstSelection = none { it.selected }
                    set(selectedIndex, selectedItem.copy(selected = selected))
                    selectedChapterIds.addOrRemove(item.id, selected)

                    if (selected && fromLongPress) {
                        if (firstSelection) {
                            selectedPositions[0] = selectedIndex
                            selectedPositions[1] = selectedIndex
                        } else {
                            // Try to select the items in-between when possible
                            val range: IntRange
                            if (selectedIndex < selectedPositions[0]) {
                                range = selectedIndex + 1..<selectedPositions[0]
                                selectedPositions[0] = selectedIndex
                            } else if (selectedIndex > selectedPositions[1]) {
                                range = (selectedPositions[1] + 1)..<selectedIndex
                                selectedPositions[1] = selectedIndex
                            } else {
                                // Just select itself
                                range = IntRange.EMPTY
                            }

                            range.forEach {
                                val inbetweenItem = get(it)
                                if (!inbetweenItem.selected) {
                                    selectedChapterIds.add(inbetweenItem.id)
                                    set(it, inbetweenItem.copy(selected = true))
                                }
                            }
                        }
                    } else if (!fromLongPress) {
                        if (!selected) {
                            if (selectedIndex == selectedPositions[0]) {
                                selectedPositions[0] = indexOfFirst { it.selected }
                            } else if (selectedIndex == selectedPositions[1]) {
                                selectedPositions[1] = indexOfLast { it.selected }
                            }
                        } else {
                            if (selectedIndex < selectedPositions[0]) {
                                selectedPositions[0] = selectedIndex
                            } else if (selectedIndex > selectedPositions[1]) {
                                selectedPositions[1] = selectedIndex
                            }
                        }
                    }
                }
            }
        }
    }

    fun setAll(chapters: List<ChapterList.Item>, selected: Boolean): List<ChapterList.Item> {
        val newChapters = chapters.map {
            selectedChapterIds.addOrRemove(it.id, selected)
            it.copy(selected = selected)
        }
        selectedPositions[0] = -1
        selectedPositions[1] = -1
        return newChapters
    }

    fun invert(chapters: List<ChapterList.Item>): List<ChapterList.Item> {
        val newChapters = chapters.map {
            selectedChapterIds.addOrRemove(it.id, !it.selected)
            it.copy(selected = !it.selected)
        }
        selectedPositions[0] = -1
        selectedPositions[1] = -1
        return newChapters
    }
}
