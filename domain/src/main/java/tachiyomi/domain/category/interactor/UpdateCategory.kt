package tachiyomi.domain.category.interactor

import tachiyomi.core.common.util.lang.withNonCancellableContext
import tachiyomi.domain.category.model.CategoryUpdate
import tachiyomi.domain.category.repository.CategoryRepository

/** Applies a [CategoryUpdate]. */
public class UpdateCategory(
    private val categoryRepository: CategoryRepository,
) {

    /** Writes the non-null fields of [payload]; a store failure is returned as [Result.Error], not logged. */
    public suspend fun await(payload: CategoryUpdate): Result = withNonCancellableContext {
        try {
            categoryRepository.updatePartial(payload)
            Result.Success
        } catch (expected: Exception) {
            // Any failure of the store degrades to the fallback below.
            Result.Error(expected)
        }
    }

    /** Outcome of [await]. */
    public sealed interface Result {
        /** The update was written. */
        public data object Success : Result

        /**
         * The store threw.
         *
         * @property error What it threw.
         */
        public data class Error(val error: Exception) : Result
    }
}
