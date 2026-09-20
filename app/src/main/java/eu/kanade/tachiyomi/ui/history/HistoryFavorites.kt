package eu.kanade.tachiyomi.ui.history

import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.ui.history.HistoryScreenModel.Dialog
import kotlinx.coroutines.flow.map
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.manga.model.Manga
import uy.kohesive.injekt.api.get

/**
 * Get user categories.
 *
 * @return List of categories, not including the default category
 */
internal suspend fun HistoryScreenModel.getCategories(): List<Category> =
    getCategories.await().filterNot { it.isSystemCategory }

internal fun HistoryScreenModel.moveMangaToCategory(mangaId: Long, categories: Category?) {
    val categoryIds = listOfNotNull(categories).map { it.id }
    moveMangaToCategory(mangaId, categoryIds)
}

internal fun HistoryScreenModel.moveMangaToCategory(mangaId: Long, categoryIds: List<Long>) {
    screenModelScope.launchIO {
        setMangaCategories.await(mangaId, categoryIds)
    }
}

internal fun HistoryScreenModel.addToLibraryInCategories(manga: Manga, categories: List<Long>) {
    moveMangaToCategory(manga.id, categories)
    if (manga.favorite) return

    screenModelScope.launchIO {
        updateManga.awaitUpdateFavorite(manga.id, true)
    }
}

internal suspend fun HistoryScreenModel.getMangaCategoryIds(manga: Manga): List<Long> {
    return getCategories.await(manga.id)
        .map { it.id }
}

internal fun HistoryScreenModel.addFavorite(mangaId: Long) {
    screenModelScope.launchIO {
        val manga = getManga.await(mangaId) ?: return@launchIO

        val duplicates = getDuplicateLibraryManga(manga)
        if (duplicates.isNotEmpty()) {
            updateState { it.copy(dialog = Dialog.DuplicateManga(manga, duplicates)) }
            return@launchIO
        }

        addFavorite(manga)
    }
}

internal fun HistoryScreenModel.addFavorite(manga: Manga) {
    screenModelScope.launchIO { doAddFavorite(manga = manga) }
}

private suspend fun HistoryScreenModel.doAddFavorite(manga: Manga) {
    // Move to default category if applicable
    val categories = getCategories()
    val defaultCategoryId = libraryPreferences.defaultCategory.get().toLong()
    val defaultCategory = categories.find { it.id == defaultCategoryId }

    when {
        // Default category set
        defaultCategory != null -> {
            val result = updateManga.awaitUpdateFavorite(manga.id, true)
            if (!result) return
            moveMangaToCategory(manga.id, defaultCategory)
        }

        // Automatic 'Default' or no categories
        defaultCategoryId == 0L || categories.isEmpty() -> {
            val result = updateManga.awaitUpdateFavorite(manga.id, true)
            if (!result) return
            moveMangaToCategory(manga.id, null)
        }

        // Choose a category
        else -> {
            showChangeCategoryDialog(manga)
        }
    }

    // Sync with tracking services if applicable
    addTracks.bindEnhancedTrackers(manga, sourceManager.getOrStub(manga.source))
}
