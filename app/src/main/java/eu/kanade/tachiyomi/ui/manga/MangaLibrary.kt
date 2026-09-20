package eu.kanade.tachiyomi.ui.manga

import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.manga.interactor.UpdateManga
import eu.kanade.domain.track.interactor.AddTracks
import eu.kanade.tachiyomi.ui.manga.MangaScreenModel.Dialog
import eu.kanade.tachiyomi.util.removeCovers
import kotlinx.coroutines.launch
import tachiyomi.core.common.preference.mapAsCheckboxState
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.interactor.SetMangaCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.interactor.GetDuplicateLibraryManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.MangaRepository
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * The library side of the manga screen: adding to / removing from the library, the category
 * membership and the fetch interval. Composed by [MangaScreenModel], whose state it updates.
 */
internal class MangaLibrary(
    private val model: MangaScreenModel,
    private val mangaId: Long,
    private val libraryPreferences: LibraryPreferences,
    private val updateManga: UpdateManga = Injekt.get(),
    private val getDuplicateLibraryManga: GetDuplicateLibraryManga = Injekt.get(),
    private val getCategories: GetCategories = Injekt.get(),
    private val addTracks: AddTracks = Injekt.get(),
    private val setMangaCategories: SetMangaCategories = Injekt.get(),
    private val mangaRepository: MangaRepository = Injekt.get(),
) {
    /**
     * Update favorite status of manga, (removes / adds) manga (to / from) library.
     */
    fun toggleFavorite(
        onRemoved: () -> Unit,
        checkDuplicate: Boolean = true,
    ) {
        val state = model.successState ?: return
        model.screenModelScope.launchIO {
            val manga = state.manga

            if (model.isFavorited) {
                // Remove from library
                if (updateManga.awaitUpdateFavorite(manga.id, false)) {
                    // Remove covers and update last modified in db
                    if (manga.removeCovers() != manga) {
                        updateManga.awaitUpdateCoverLastModified(manga.id)
                    }
                    withUIContext { onRemoved() }
                }
            } else {
                // Add to library
                // First, check if duplicate exists if callback is provided
                if (checkDuplicate) {
                    val duplicates = getDuplicateLibraryManga(manga)

                    if (duplicates.isNotEmpty()) {
                        model.updateSuccessState { it.copy(dialog = Dialog.DuplicateManga(manga, duplicates)) }
                        return@launchIO
                    }
                }

                // Now check if user previously set categories, when available
                val categories = getCategories()
                val defaultCategoryId = libraryPreferences.defaultCategory.get().toLong()
                val defaultCategory = categories.find { it.id == defaultCategoryId }
                when {
                    // Default category set
                    defaultCategory != null -> {
                        val result = updateManga.awaitUpdateFavorite(manga.id, true)
                        if (!result) return@launchIO
                        moveMangaToCategory(defaultCategory)
                    }

                    // Automatic 'Default' or no categories
                    defaultCategoryId == 0L || categories.isEmpty() -> {
                        val result = updateManga.awaitUpdateFavorite(manga.id, true)
                        if (!result) return@launchIO
                        moveMangaToCategory(null)
                    }

                    // Choose a category
                    else -> {
                        showChangeCategoryDialog()
                    }
                }

                // Finally match with enhanced tracking when available
                addTracks.bindEnhancedTrackers(manga, state.source)
            }
        }
    }

    fun showChangeCategoryDialog() {
        val manga = model.successState?.manga ?: return
        model.screenModelScope.launch {
            val categories = getCategories()
            val selection = getMangaCategoryIds(manga)
            model.updateSuccessState { successState ->
                successState.copy(
                    dialog = Dialog.ChangeCategory(
                        manga = manga,
                        initialSelection = categories.mapAsCheckboxState { it.id in selection },
                    ),
                )
            }
        }
    }

    fun showSetFetchIntervalDialog() {
        val manga = model.successState?.manga ?: return
        model.updateSuccessState {
            it.copy(dialog = Dialog.SetFetchInterval(manga))
        }
    }

    fun setFetchInterval(manga: Manga, interval: Int) {
        model.screenModelScope.launchIO {
            if (
                updateManga.awaitUpdateFetchInterval(
                    // Custom intervals are negative
                    manga.copy(fetchInterval = -interval),
                )
            ) {
                val updatedManga = mangaRepository.getMangaById(manga.id)
                model.updateSuccessState { it.copy(manga = updatedManga) }
            }
        }
    }

    /**
     * Get user categories.
     *
     * @return List of categories, not including the default category
     */
    suspend fun getCategories(): List<Category> = getCategories.await().filterNot { it.isSystemCategory }

    // Gets the category id's the manga is in, if the manga is not in a category, returns the default id.
    // @param manga the manga to get categories from.
    // @return Array of category ids the manga is in, if none returns default id
    private suspend fun getMangaCategoryIds(manga: Manga): List<Long> {
        return getCategories.await(manga.id)
            .map { it.id }
    }

    fun addToLibraryInCategories(manga: Manga, categories: List<Long>) {
        moveMangaToCategory(categories)
        if (manga.favorite) return

        model.screenModelScope.launchIO {
            updateManga.awaitUpdateFavorite(manga.id, true)
        }
    }

    // Move the given manga to categories.
    // @param categories the selected categories.
    private fun moveMangaToCategories(categories: List<Category>) {
        val categoryIds = categories.map { it.id }
        moveMangaToCategory(categoryIds)
    }

    private fun moveMangaToCategory(categoryIds: List<Long>) {
        model.screenModelScope.launchIO {
            setMangaCategories.await(mangaId, categoryIds)
        }
    }

    // Move the given manga to the category.
    // @param category the selected category, or null for default category.
    private fun moveMangaToCategory(category: Category?) {
        moveMangaToCategories(listOfNotNull(category))
    }
}
