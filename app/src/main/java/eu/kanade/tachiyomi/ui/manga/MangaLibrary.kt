package eu.kanade.tachiyomi.ui.manga

import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.manga.interactor.UpdateManga
import eu.kanade.domain.track.interactor.AddTracks
import eu.kanade.tachiyomi.source.Source
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
            if (model.isFavorited) {
                removeFromLibrary(state.manga, onRemoved)
            } else {
                addToLibrary(state.manga, state.source, checkDuplicate)
            }
        }
    }

    private suspend fun removeFromLibrary(manga: Manga, onRemoved: () -> Unit) {
        if (!updateManga.awaitUpdateFavorite(manga.id, false)) return
        // Remove covers and update last modified in db
        if (manga.removeCovers() != manga) {
            updateManga.awaitUpdateCoverLastModified(manga.id)
        }
        withUIContext { onRemoved() }
    }

    // A duplicate (when checked) or a missing default category stops at a dialog; otherwise the entry is
    // favourited, filed, and matched with enhanced trackers.
    private suspend fun addToLibrary(manga: Manga, source: Source, checkDuplicate: Boolean) {
        val duplicates = if (checkDuplicate) getDuplicateLibraryManga(manga) else emptyList()
        if (duplicates.isNotEmpty()) {
            model.updateSuccessState { it.copy(dialog = Dialog.DuplicateManga(manga, duplicates)) }
            return
        }

        // Now check if user previously set categories, when available
        val categories = getCategories()
        val defaultCategoryId = libraryPreferences.defaultCategory.get().toLong()
        val defaultCategory = categories.find { it.id == defaultCategoryId }
        val filed = when {
            // Default category set
            defaultCategory != null -> {
                favouriteInto(manga, defaultCategory)
            }
            // Automatic 'Default' or no categories
            defaultCategoryId == 0L || categories.isEmpty() -> {
                favouriteInto(manga, null)
            }
            // Choose a category
            else -> {
                showChangeCategoryDialog()
                true
            }
        }
        if (!filed) return

        // Finally match with enhanced tracking when available
        addTracks.bindEnhancedTrackers(manga, source)
    }

    // False when the favourite flag could not be written, in which case nothing else happens.
    private suspend fun favouriteInto(manga: Manga, category: Category?): Boolean {
        val result = updateManga.awaitUpdateFavorite(manga.id, true)
        if (result) moveMangaToCategory(listOfNotNull(category).map { it.id })
        return result
    }

    fun showChangeCategoryDialog() {
        val manga = model.successState?.manga ?: return
        model.screenModelScope.launch {
            val categories = getCategories()
            val selection = getCategories.await(manga.id).map { it.id }
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

    fun addToLibraryInCategories(manga: Manga, categories: List<Long>) {
        moveMangaToCategory(categories)
        if (manga.favorite) return

        model.screenModelScope.launchIO {
            updateManga.awaitUpdateFavorite(manga.id, true)
        }
    }

    private fun moveMangaToCategory(categoryIds: List<Long>) {
        model.screenModelScope.launchIO {
            setMangaCategories.await(mangaId, categoryIds)
        }
    }
}
