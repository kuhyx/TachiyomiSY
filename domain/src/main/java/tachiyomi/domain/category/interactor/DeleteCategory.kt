package tachiyomi.domain.category.interactor

import logcat.LogPriority
import tachiyomi.core.common.util.lang.withNonCancellableContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.category.model.CategoryUpdate
import tachiyomi.domain.category.repository.CategoryRepository
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.library.service.LibraryPreferences

/** Deletes a category, forgets it in every preference that names categories, and closes the order gap. */
public class DeleteCategory(
    private val categoryRepository: CategoryRepository,
    private val libraryPreferences: LibraryPreferences,
    private val downloadPreferences: DownloadPreferences,
) {

    /** Deletes the category; a store failure is logged and returned as [Result.InternalError]. */
    public suspend fun await(categoryId: Long): Result = withNonCancellableContext {
        try {
            categoryRepository.delete(categoryId)
            forgetCategory(categoryId)
            categoryRepository.updatePartial(reorderedUpdates())
            Result.Success
        } catch (expected: Exception) {
            // Any failure of the store is logged and reported as the fallback below.
            logcat(LogPriority.ERROR, expected)
            Result.InternalError(expected)
        }
    }

    // Closes the gap the deleted category left in the order.
    private suspend fun reorderedUpdates(): List<CategoryUpdate> {
        return categoryRepository.getAll().mapIndexed { index, category ->
            CategoryUpdate(
                id = category.id,
                order = index.toLong(),
            )
        }
    }

    // Drops the category from every preference that names categories.
    private fun forgetCategory(categoryId: Long) {
        if (libraryPreferences.defaultCategory.get() == categoryId.toInt()) {
            libraryPreferences.defaultCategory.delete()
        }
        val categoryPreferences = listOf(
            libraryPreferences.updateCategories,
            libraryPreferences.updateCategoriesExclude,
            downloadPreferences.removeExcludeCategories,
            downloadPreferences.downloadNewChapterCategories,
            downloadPreferences.downloadNewChapterCategoriesExclude,
        )
        val categoryIdString = categoryId.toString()
        categoryPreferences.forEach { preference ->
            val ids = preference.get()
            if (categoryIdString in ids) {
                preference.set(ids - categoryIdString)
            }
        }
    }

    /** Outcome of [await]. */
    public sealed interface Result {
        /** The category is gone. */
        public data object Success : Result

        /**
         * The store threw.
         *
         * @property error What it threw.
         */
        public data class InternalError(val error: Throwable) : Result
    }
}
