package eu.kanade.tachiyomi.ui.library

import androidx.compose.ui.util.fastMap
import eu.kanade.tachiyomi.ui.library.LibraryScreenModel.State
import mihon.core.common.utils.mutate
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.model.LibraryManga

/**
 * The library multi-select: every operation maps the screen [State] to the state with the new
 * selection, and remembers the category of the last press for range selection. Composed by
 * [LibraryScreenModel].
 */
internal class LibrarySelection {
    private var lastSelectionCategory: Long? = null

    fun clear(state: State): State {
        lastSelectionCategory = null
        return state.copy(selection = setOf())
    }

    fun toggle(state: State, category: Category, manga: LibraryManga): State {
        val newSelection = state.selection.mutate { set ->
            if (!set.remove(manga.id)) set.add(manga.id)
        }
        lastSelectionCategory = category.id.takeIf { newSelection.isNotEmpty() }
        return state.copy(selection = newSelection)
    }

    /**
     * Selects all mangas between and including the given manga and the last pressed manga from the
     * same category as the given manga.
     */
    fun toggleRange(state: State, category: Category, manga: LibraryManga): State {
        val newSelection = state.selection.mutate { list ->
            val lastSelected = list.lastOrNull()
            if (lastSelectionCategory != category.id) {
                list.add(manga.id)
                return@mutate
            }

            val items = state.getItemsForCategoryId(category.id).fastMap { it.id }
            val lastMangaIndex = items.indexOf(lastSelected)
            val curMangaIndex = items.indexOf(manga.id)

            val selectionRange = when {
                lastMangaIndex < curMangaIndex -> lastMangaIndex..curMangaIndex
                curMangaIndex < lastMangaIndex -> curMangaIndex..lastMangaIndex
                // We shouldn't reach this point
                else -> return@mutate
            }
            selectionRange.mapNotNull { items[it] }.let(list::addAll)
        }
        lastSelectionCategory = category.id
        return state.copy(selection = newSelection)
    }

    fun selectAll(state: State): State {
        lastSelectionCategory = null
        val newSelection = state.selection.mutate { list ->
            state.getItemsForCategoryId(state.activeCategory?.id).map { it.id }.let(list::addAll)
        }
        return state.copy(selection = newSelection)
    }

    fun invert(state: State): State {
        lastSelectionCategory = null
        val newSelection = state.selection.mutate { list ->
            val itemIds = state.getItemsForCategoryId(state.activeCategory?.id).fastMap { it.id }
            val (toRemove, toAdd) = itemIds.partition { it in list }
            list.removeAll(toRemove.toSet())
            list.addAll(toAdd)
        }
        return state.copy(selection = newSelection)
    }
}
