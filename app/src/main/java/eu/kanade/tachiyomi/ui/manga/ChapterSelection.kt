package eu.kanade.tachiyomi.ui.manga

import eu.kanade.core.util.addOrRemove
import eu.kanade.core.util.toggleRow

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
    ): List<ChapterList.Item> = chapters.toMutableList().apply {
        toggleRow(
            selectedIndex = indexOfFirst { it.id == item.chapter.id },
            selected = selected,
            fromLongPress = fromLongPress,
            positions = selectedPositions,
            isSelected = { it.selected },
            withSelected = { chapter, value -> chapter.copy(selected = value) },
            track = { chapter, value -> selectedChapterIds.addOrRemove(chapter.id, value) },
        )
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
