package tachiyomi.domain.category.interactor

import logcat.LogPriority
import tachiyomi.core.common.util.lang.withNonCancellableContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.repository.CategoryRepository
import tachiyomi.domain.library.service.LibraryPreferences

/** Creates a category at the end of the list with the current library sort as its flags. */
public class CreateCategoryWithName(
    private val categoryRepository: CategoryRepository,
    private val preferences: LibraryPreferences,
) {

    private val initialFlags: Long
        get() {
            val sort = preferences.sortingMode.get()
            return sort.type.flag or sort.direction.flag
        }

    /** Inserts a category called [name]; a store failure is logged and returned as [Result.InternalError]. */
    public suspend fun await(name: String): Result = withNonCancellableContext {
        val categories = categoryRepository.getAll()
        val nextOrder = categories.maxOfOrNull { it.order }?.plus(1) ?: 0
        val newCategory = Category(
            id = 0,
            name = name,
            order = nextOrder,
            flags = initialFlags,
        )

        try {
            categoryRepository.insert(newCategory)
            Result.Success(/* SY --> */newCategory/* SY <-- */)
        } catch (expected: Exception) {
            // Any failure of the store is logged and reported as the fallback below.
            logcat(LogPriority.ERROR, expected)
            Result.InternalError(expected)
        }
    }

    /** Outcome of [await]. */
    public sealed interface Result {
        // SY -->
        /**
         * The category was inserted.
         *
         * @property category The category as it was inserted; its [Category.id] is still 0.
         */
        public data class Success(val category: Category) : Result

        // SY <--

        /**
         * The store threw.
         *
         * @property error What it threw.
         */
        public data class InternalError(val error: Throwable) : Result
    }
}
