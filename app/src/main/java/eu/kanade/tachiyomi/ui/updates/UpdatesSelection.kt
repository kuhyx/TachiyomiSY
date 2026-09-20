package eu.kanade.tachiyomi.ui.updates

import androidx.compose.runtime.getValue
import eu.kanade.core.util.addOrRemove
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import uy.kohesive.injekt.api.get

internal fun UpdatesScreenModel.toggleSelection(
    item: UpdatesItem,
    selected: Boolean,
    fromLongPress: Boolean = false,
) {
    updateState { state ->
        val newItems = state.items.toMutableList().apply {
            val selectedIndex = indexOfFirst { it.update.chapterId == item.update.chapterId }
            if (!(selectedIndex < 0)) {
                val selectedItem = get(selectedIndex)
                if (selectedItem.selected != selected) {
                    val firstSelection = none { it.selected }
                    set(selectedIndex, selectedItem.copy(selected = selected))
                    selectedChapterIds.addOrRemove(item.update.chapterId, selected)

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
                                    selectedChapterIds.add(inbetweenItem.update.chapterId)
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
        state.copy(items = newItems)
    }
}

internal fun UpdatesScreenModel.toggleAllSelection(selected: Boolean) {
    updateState { state ->
        val newItems = state.items.map {
            selectedChapterIds.addOrRemove(it.update.chapterId, selected)
            it.copy(selected = selected)
        }
        state.copy(items = newItems)
    }

    selectedPositions[0] = -1
    selectedPositions[1] = -1
}

internal fun UpdatesScreenModel.invertSelection() {
    updateState { state ->
        val newItems = state.items.map {
            selectedChapterIds.addOrRemove(it.update.chapterId, !it.selected)
            it.copy(selected = !it.selected)
        }
        state.copy(items = newItems)
    }
    selectedPositions[0] = -1
    selectedPositions[1] = -1
}
