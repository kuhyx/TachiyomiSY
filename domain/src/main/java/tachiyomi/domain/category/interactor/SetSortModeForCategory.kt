package tachiyomi.domain.category.interactor

import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.model.CategoryUpdate
import tachiyomi.domain.category.repository.CategoryRepository
import tachiyomi.domain.library.model.LibraryGroup
import tachiyomi.domain.library.model.LibrarySort
import tachiyomi.domain.library.model.plus
import tachiyomi.domain.library.service.LibraryPreferences
import kotlin.random.Random

/** Changes the library sort, per category or globally depending on the display-settings preferences. */
public class SetSortModeForCategory(
    private val preferences: LibraryPreferences,
    private val categoryRepository: CategoryRepository,
) {

    /**
     * Writes the sort to the category's flags when per-category settings are on and the category
     * exists; otherwise to the global preference and every category. When the library is grouped
     * by anything but categories only the global preference changes. Store failures propagate.
     */
    public suspend fun await(categoryId: Long?, type: LibrarySort.Type, direction: LibrarySort.Direction) {
        // SY -->
        if (preferences.groupLibraryBy.get() != LibraryGroup.BY_DEFAULT) {
            preferences.sortingMode.set(LibrarySort(type, direction))
            return
        }
        // SY <--
        val category = categoryId?.let { categoryRepository.get(it) }
        val flags = (category?.flags ?: 0) + type + direction
        if (type == LibrarySort.Type.Random) {
            preferences.randomSortSeed.set(Random.nextInt())
        }
        if (category != null && preferences.categorizedDisplaySettings.get()) {
            categoryRepository.updatePartial(
                CategoryUpdate(
                    id = category.id,
                    flags = flags,
                ),
            )
        } else {
            preferences.sortingMode.set(LibrarySort(type, direction))
            categoryRepository.updateAllFlags(flags)
        }
    }

    /** [await] by [Category.id]; a null category means the global sort. */
    public suspend fun await(
        category: Category?,
        type: LibrarySort.Type,
        direction: LibrarySort.Direction,
    ) {
        await(category?.id, type, direction)
    }
}
