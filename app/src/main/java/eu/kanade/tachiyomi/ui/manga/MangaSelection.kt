package eu.kanade.tachiyomi.ui.manga

import androidx.compose.runtime.getValue
import uy.kohesive.injekt.api.get

internal fun MangaScreenModel.toggleSelection(
    item: ChapterList.Item,
    selected: Boolean,
    fromLongPress: Boolean = false,
) {
    updateSuccessState { successState ->
        val (chapters, selection) =
            successState.selection.toggle(successState.processedChapters, item, selected, fromLongPress)
        successState.copy(chapters = chapters, selection = selection)
    }
}

internal fun MangaScreenModel.toggleAllSelection(selected: Boolean) {
    updateSuccessState { successState ->
        val (chapters, selection) = successState.selection.setAll(successState.chapters, selected)
        successState.copy(chapters = chapters, selection = selection)
    }
}

internal fun MangaScreenModel.invertSelection() {
    updateSuccessState { successState ->
        val (chapters, selection) = successState.selection.invert(successState.chapters)
        successState.copy(chapters = chapters, selection = selection)
    }
}
