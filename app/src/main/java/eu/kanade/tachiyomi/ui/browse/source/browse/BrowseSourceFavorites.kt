package eu.kanade.tachiyomi.ui.browse.source.browse

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.paging.filter
import androidx.paging.map
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreenModel.Dialog
import eu.kanade.tachiyomi.util.removeCovers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import tachiyomi.core.common.preference.mapAsCheckboxState
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.toMangaUpdate
import uy.kohesive.injekt.api.get
import java.time.Instant

/**
 * Adds or removes a manga from the library.
 *
 * @param manga the manga to update.
 */
internal fun BrowseSourceScreenModel.changeMangaFavorite(manga: Manga) {
    screenModelScope.launch {
        var new = manga.copy(
            favorite = !manga.favorite,
            dateAdded = when (manga.favorite) {
                true -> 0
                false -> Instant.now().toEpochMilli()
            },
        )

        if (!new.favorite) {
            new = new.removeCovers(coverCache)
        } else {
            setMangaDefaultChapterFlags.await(manga)
            addTracks.bindEnhancedTrackers(manga, source)
        }

        updateManga.await(new.toMangaUpdate())
    }
}

internal fun BrowseSourceScreenModel.addFavorite(manga: Manga) {
    screenModelScope.launch {
        val categories = getCategories()
        val defaultCategoryId = libraryPreferences.defaultCategory.get()
        val defaultCategory = categories.find { it.id == defaultCategoryId.toLong() }

        when {
            // Default category set
            defaultCategory != null -> {
                moveMangaToCategories(manga, defaultCategory)

                changeMangaFavorite(manga)
            }

            // Automatic 'Default' or no categories
            defaultCategoryId == 0 || categories.isEmpty() -> {
                moveMangaToCategories(manga)

                changeMangaFavorite(manga)
            }

            // Choose a category
            else -> {
                val preselectedIds = getCategories.await(manga.id).map { it.id }
                setDialog(
                    Dialog.ChangeMangaCategory(
                        manga,
                        categories.mapAsCheckboxState { it.id in preselectedIds },
                    ),
                )
            }
        }
    }
}

/**
 * Get user categories.
 *
 * @return List of categories, not including the default category
 */
internal suspend fun BrowseSourceScreenModel.getCategories(): List<Category> {
    return getCategories.subscribe()
        .firstOrNull()
        ?.filterNot { it.isSystemCategory }
        .orEmpty()
}

internal fun BrowseSourceScreenModel.moveMangaToCategories(manga: Manga, vararg categories: Category) {
    moveMangaToCategories(manga, categories.filter { it.id != 0L }.map { it.id })
}

internal fun BrowseSourceScreenModel.moveMangaToCategories(manga: Manga, categoryIds: List<Long>) {
    screenModelScope.launchIO {
        setMangaCategories.await(
            mangaId = manga.id,
            categoryIds = categoryIds.toList(),
        )
    }
}
