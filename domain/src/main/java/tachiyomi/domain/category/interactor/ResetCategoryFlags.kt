package tachiyomi.domain.category.interactor

import tachiyomi.domain.category.repository.CategoryRepository
import tachiyomi.domain.library.model.plus
import tachiyomi.domain.library.service.LibraryPreferences

/** Gives every category the global library sort, dropping per-category sorts. */
public class ResetCategoryFlags(
    private val preferences: LibraryPreferences,
    private val categoryRepository: CategoryRepository,
) {

    /** Writes the `sortingMode` preference into every category's flags; store failures propagate. */
    public suspend fun await() {
        val sort = preferences.sortingMode.get()
        categoryRepository.updateAllFlags(sort.type + sort.direction)
    }
}
