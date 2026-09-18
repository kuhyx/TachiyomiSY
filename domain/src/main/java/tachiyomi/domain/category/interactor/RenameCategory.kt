package tachiyomi.domain.category.interactor

import logcat.LogPriority
import tachiyomi.core.common.util.lang.withNonCancellableContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.model.CategoryUpdate
import tachiyomi.domain.category.repository.CategoryRepository

/** Changes a category's display name. */
public class RenameCategory(
    private val categoryRepository: CategoryRepository,
) {

    /** Renames the category to [name]; a store failure is logged and returned as [Result.InternalError]. */
    public suspend fun await(categoryId: Long, name: String): Result = withNonCancellableContext {
        val update = CategoryUpdate(
            id = categoryId,
            name = name,
        )

        try {
            categoryRepository.updatePartial(update)
            Result.Success
        } catch (expected: Exception) {
            // Any failure of the store is logged and reported as the fallback below.
            logcat(LogPriority.ERROR, expected)
            Result.InternalError(expected)
        }
    }

    /** [await] by [Category.id]. */
    public suspend fun await(category: Category, name: String): Result = await(category.id, name)

    /** Outcome of [await]. */
    public sealed interface Result {
        /** The name was written. */
        public data object Success : Result

        /**
         * The store threw.
         *
         * @property error What it threw.
         */
        public data class InternalError(val error: Throwable) : Result
    }
}
