package eu.kanade.core.util

/**
 * Toggles the row at [selectedIndex] of a multi-select list in place. A long press that selects extends
 * the selection across every row between it and the rows already selected; [positions] holds the first
 * and last selected index that range grows from (-1 when nothing is selected). A row that is missing or
 * already in the requested state is left alone.
 *
 * [track] is told about every row whose state changes so the caller can keep its id set current.
 */
internal fun <T> MutableList<T>.toggleRow(
    selectedIndex: Int,
    selected: Boolean,
    fromLongPress: Boolean,
    positions: Array<Int>,
    isSelected: (T) -> Boolean,
    withSelected: (T, Boolean) -> T,
    track: (T, Boolean) -> Unit,
) {
    if (selectedIndex < 0) return
    val selectedItem = get(selectedIndex)
    if (isSelected(selectedItem) == selected) return

    val firstSelection = none(isSelected)
    set(selectedIndex, withSelected(selectedItem, selected))
    track(selectedItem, selected)

    when {
        selected && fromLongPress -> {
            extendRange(selectedIndex, firstSelection, positions, isSelected, withSelected, track)
        }
        !fromLongPress -> {
            adjustPositions(selectedIndex, selected, positions, isSelected)
        }
    }
}

private fun <T> MutableList<T>.extendRange(
    selectedIndex: Int,
    firstSelection: Boolean,
    positions: Array<Int>,
    isSelected: (T) -> Boolean,
    withSelected: (T, Boolean) -> T,
    track: (T, Boolean) -> Unit,
) {
    if (firstSelection) {
        positions[0] = selectedIndex
        positions[1] = selectedIndex
        return
    }
    // Try to select the items in-between when possible
    val range = when {
        selectedIndex < positions[0] -> (selectedIndex + 1..<positions[0]).also { positions[0] = selectedIndex }
        selectedIndex > positions[1] -> ((positions[1] + 1)..<selectedIndex).also { positions[1] = selectedIndex }
        // Just select itself
        else -> IntRange.EMPTY
    }
    range.forEach { index ->
        val inbetweenItem = get(index)
        if (!isSelected(inbetweenItem)) {
            track(inbetweenItem, true)
            set(index, withSelected(inbetweenItem, true))
        }
    }
}

// A plain tap only moves the range ends: outward on select, inward to the next selected row on deselect.
private fun <T> List<T>.adjustPositions(
    selectedIndex: Int,
    selected: Boolean,
    positions: Array<Int>,
    isSelected: (T) -> Boolean,
) {
    when {
        !selected && selectedIndex == positions[0] -> positions[0] = indexOfFirst(isSelected)
        !selected && selectedIndex == positions[1] -> positions[1] = indexOfLast(isSelected)
        selected && selectedIndex < positions[0] -> positions[0] = selectedIndex
        selected && selectedIndex > positions[1] -> positions[1] = selectedIndex
    }
}
