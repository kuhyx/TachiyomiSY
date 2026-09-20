package eu.kanade.tachiyomi.ui.library

import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.ui.library.LibraryScreenModel.Dialog
import eu.kanade.tachiyomi.ui.library.LibraryScreenModel.State
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import tachiyomi.core.common.preference.CheckboxState
import tachiyomi.core.common.preference.TriState
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.manga.model.Manga
import uy.kohesive.injekt.api.get

// Returns the common categories for the given list of manga.
// @param mangas the list of manga.
internal suspend fun LibraryScreenModel.getCommonCategories(mangas: List<Manga>): Collection<Category> {
    if (mangas.isEmpty()) return emptyList()
    return mangas
        .map { getCategories.await(it.id).toSet() }
        .reduce { set1, set2 -> set1.intersect(set2) }
}

// Returns the mix (non-common) categories for the given list of manga.
// @param mangas the list of manga.
internal suspend fun LibraryScreenModel.getMixCategories(mangas: List<Manga>): Collection<Category> {
    if (mangas.isEmpty()) return emptyList()
    val mangaCategories = mangas.map { getCategories.await(it.id).toSet() }
    val common = mangaCategories.reduce { set1, set2 -> set1.intersect(set2) }
    return mangaCategories.flatten().distinct().subtract(common)
}

internal fun LibraryScreenModel.showSettingsDialog() {
    updateState { it.copy(dialog = Dialog.SettingsSheet) }
}

// SY -->
internal fun LibraryScreenModel.showRecommendationSearchDialog() {
    val mangaList = state.value.selectedManga
    updateState { it.copy(dialog = Dialog.RecommendationSearchSheet(mangaList)) }
}

internal fun LibraryScreenModel.openChangeCategoryDialog() {
    screenModelScope.launchIO {
        // Create a copy of selected manga
        val mangaList = state.value.selectedManga

        // Hide the default category because it has a different behavior than the ones from db.
        // SY -->
        val categories = state.value.libraryData.categories.filter { it.id != 0L }
        // SY <--

        // Get indexes of the common categories to preselect.
        val common = getCommonCategories(mangaList)
        // Get indexes of the mix categories to preselect.
        val mix = getMixCategories(mangaList)
        val preselected = categories
            .map {
                when (it) {
                    in common -> CheckboxState.State.Checked(it)
                    in mix -> CheckboxState.TriState.Exclude(it)
                    else -> CheckboxState.State.None(it)
                }
            }

        updateState { it.copy(dialog = Dialog.ChangeCategory(mangaList, preselected)) }
    }
}

internal fun LibraryScreenModel.openDeleteMangaDialog() {
    updateState { it.copy(dialog = Dialog.DeleteManga(state.value.selectedManga)) }
}

internal fun LibraryScreenModel.closeDialog() {
    updateState { it.copy(dialog = null) }
}

internal fun LibraryScreenModel.openFavoritesSyncDialog() {
    updateState {
        it.copy(
            dialog = if (exhPreferences.exhShowSyncIntro.get()) {
                Dialog.SyncFavoritesWarning
            } else {
                Dialog.SyncFavoritesConfirm
            },
        )
    }
}
