package eu.kanade.tachiyomi.ui.updates

import androidx.compose.runtime.getValue
import eu.kanade.core.util.addOrRemove
import eu.kanade.core.util.toggleRow
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
            toggleRow(
                selectedIndex = indexOfFirst { it.update.chapterId == item.update.chapterId },
                selected = selected,
                fromLongPress = fromLongPress,
                positions = selectedPositions,
                isSelected = { it.selected },
                withSelected = { row, value -> row.copy(selected = value) },
                track = { row, value -> selectedChapterIds.addOrRemove(row.update.chapterId, value) },
            )
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
