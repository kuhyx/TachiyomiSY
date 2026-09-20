package eu.kanade.tachiyomi.ui.manga

import androidx.compose.runtime.getValue
import uy.kohesive.injekt.api.get

internal fun MangaScreenModel.toggleSelection(
    item: ChapterList.Item,
    selected: Boolean,
    fromLongPress: Boolean = false,
) {
    updateSuccessState { successState ->
        val chapters = selection.toggle(successState.processedChapters, item, selected, fromLongPress)
        successState.copy(chapters = chapters)
    }
}

internal fun MangaScreenModel.toggleAllSelection(selected: Boolean) {
    updateSuccessState { successState ->
        successState.copy(chapters = selection.setAll(successState.chapters, selected))
    }
}

internal fun MangaScreenModel.invertSelection() {
    updateSuccessState { successState ->
        successState.copy(chapters = selection.invert(successState.chapters))
    }
}
