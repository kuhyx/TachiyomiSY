package tachiyomi.domain.category.interactor

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import logcat.LogPriority
import tachiyomi.core.common.util.lang.withNonCancellableContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.model.CategoryUpdate
import tachiyomi.domain.category.repository.CategoryRepository

/** Moves a category to a new position and renumbers the others; the built-in category never moves. */
public class ReorderCategory(
    private val categoryRepository: CategoryRepository,
) {
    private val mutex = Mutex()

    /**
     * Moves [category] to [newIndex] among the user categories. Returns [Result.Unchanged] if the
     * category is not in the list; a store failure is logged and returned as [Result.InternalError].
     */
    public suspend fun await(category: Category, newIndex: Int): Result = withNonCancellableContext {
        mutex.withLock {
            val categories = categoryRepository.getAll()
                .filterNot(Category::isSystemCategory)
                .toMutableList()

            val currentIndex = categories.indexOfFirst { it.id == category.id }
            if (currentIndex == -1) {
                Result.Unchanged
            } else {
                reorder(categories, currentIndex, newIndex)
            }
        }
    }

    private suspend fun reorder(categories: MutableList<Category>, currentIndex: Int, newIndex: Int): Result {
        return try {
            categories.add(newIndex, categories.removeAt(currentIndex))
            val updates = categories.mapIndexed { index, category ->
                CategoryUpdate(
                    id = category.id,
                    order = index.toLong(),
                )
            }
            categoryRepository.updatePartial(updates)
            Result.Success
        } catch (expected: Exception) {
            // Any failure of the store is logged and reported as the fallback below.
            logcat(LogPriority.ERROR, expected)
            Result.InternalError(expected)
        }
    }

    /** Outcome of [await]. */
    public sealed interface Result {
        /** The new order was written. */
        public data object Success : Result

        /** The category was not found, so nothing was written. */
        public data object Unchanged : Result

        /**
         * The store threw.
         *
         * @property error What it threw.
         */
        public data class InternalError(val error: Throwable) : Result
    }
}
