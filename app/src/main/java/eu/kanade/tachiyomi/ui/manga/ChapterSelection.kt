package eu.kanade.tachiyomi.ui.manga

import eu.kanade.core.util.addOrRemove
import eu.kanade.core.util.toggleRow

/**
 * The chapter multi-select of the manga screen, kept in the screen state as an immutable value: which
 * ids are selected and the first/last selected chapter that long-press range selection extends from.
 * The anchors are chapter ids, not list indices, so a chapter list that changes between two long
 * presses (a new chapter, a filter) still extends from the right rows. Every operation below is pure
 * and returns the updated chapter list with the selection after it, so a `MutableStateFlow.update`
 * that re-runs its lambda under contention cannot apply a toggle twice.
 */
internal data class ChapterSelection(
    val selectedChapterIds: Set<Long> = emptySet(),
    val firstAnchor: Long? = null,
    val lastAnchor: Long? = null,
)

internal fun ChapterSelection.toggle(
    chapters: List<ChapterList.Item>,
    item: ChapterList.Item,
    selected: Boolean,
    fromLongPress: Boolean,
): Pair<List<ChapterList.Item>, ChapterSelection> {
    val positions = arrayOf(chapters.indexOfId(firstAnchor), chapters.indexOfId(lastAnchor))
    val ids = selectedChapterIds.toHashSet()
    val toggled = chapters.toMutableList().apply {
        toggleRow(
            selectedIndex = indexOfFirst { it.id == item.chapter.id },
            selected = selected,
            fromLongPress = fromLongPress,
            positions = positions,
            isSelected = { it.selected },
            withSelected = { chapter, value -> chapter.copy(selected = value) },
            track = { chapter, value -> ids.addOrRemove(chapter.id, value) },
        )
    }
    val anchors = positions.map { toggled.getOrNull(it)?.id }
    return toggled to ChapterSelection(ids, firstAnchor = anchors[0], lastAnchor = anchors[1])
}

internal fun ChapterSelection.setAll(
    chapters: List<ChapterList.Item>,
    selected: Boolean,
): Pair<List<ChapterList.Item>, ChapterSelection> {
    val ids = chapters.map { it.id }.toSet()
    val selection = ChapterSelection(if (selected) selectedChapterIds + ids else selectedChapterIds - ids)
    return chapters.map { it.copy(selected = selected) } to selection
}

internal fun ChapterSelection.invert(
    chapters: List<ChapterList.Item>,
): Pair<List<ChapterList.Item>, ChapterSelection> {
    val ids = selectedChapterIds.toHashSet()
    chapters.forEach { ids.addOrRemove(it.id, !it.selected) }
    return chapters.map { it.copy(selected = !it.selected) } to ChapterSelection(ids)
}

/**
 * [chapters] with each `selected` flag matching this selection. The observer builds its list
 * outside the atomic update; applying this inside the update keeps a toggle made meanwhile.
 */
internal fun ChapterSelection.reapply(chapters: List<ChapterList.Item>): List<ChapterList.Item> = chapters.map {
    val selected = it.id in selectedChapterIds
    if (it.selected == selected) it else it.copy(selected = selected)
}

// The row of chapter [id] in this list, -1 when there is no anchor or the chapter is gone.
private fun List<ChapterList.Item>.indexOfId(id: Long?): Int = indexOfFirst { it.id == id }
