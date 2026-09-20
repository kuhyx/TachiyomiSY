package eu.kanade.tachiyomi.ui.library

import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.core.preference.PreferenceMutableState
import eu.kanade.core.preference.asState
import eu.kanade.tachiyomi.util.chapter.getNextUnread
import exh.source.MERGED_SOURCE_ID
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.manga.model.Manga
import uy.kohesive.injekt.api.get

internal suspend fun LibraryScreenModel.getNextUnreadChapter(manga: Manga): Chapter? {
    // SY -->
    val mergedManga = getMergedMangaById.await(manga.id).associateBy { it.id }
    return if (manga.id == MERGED_SOURCE_ID) {
        getMergedChaptersByMangaId.await(manga.id, applyScanlatorFilter = true)
    } else {
        getChaptersByMangaId.await(manga.id, applyScanlatorFilter = true)
    }.getNextUnread(manga, downloadManager, mergedManga)
    // SY <--
}

internal fun LibraryScreenModel.getDisplayMode(): PreferenceMutableState<LibraryDisplayMode> =
    libraryPreferences.displayMode.asState(screenModelScope)

internal fun LibraryScreenModel.getColumnsForOrientation(isLandscape: Boolean): PreferenceMutableState<Int> {
    return (if (isLandscape) libraryPreferences.landscapeColumns else libraryPreferences.portraitColumns)
        .asState(screenModelScope)
}

internal fun LibraryScreenModel.randomItemInCurrentCategory(): LibraryItem? {
    val state = state.value
    return state.getItemsForCategoryId(state.activeCategory?.id).randomOrNull()
}

internal fun LibraryScreenModel.search(query: String?) {
    updateState { it.copy(searchQuery = query) }
}

internal fun LibraryScreenModel.updateActiveCategoryIndex(index: Int) {
    updateState { it.copy(activeCategoryIndex = index) }
    libraryPreferences.lastUsedCategory.set(state.value.coercedActiveCategoryIndex)
}
