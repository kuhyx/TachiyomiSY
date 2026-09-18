package tachiyomi.domain.category.interactor

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.repository.CategoryRepository

/** Reads categories, all of them or those of one manga. */
public class GetCategories(
    private val categoryRepository: CategoryRepository,
) {

    /** Every category in order, re-emitted on every change. */
    public fun subscribe(): Flow<List<Category>> = categoryRepository.getAllAsFlow()

    /** The manga's categories in order, re-emitted on every change. */
    public fun subscribe(mangaId: Long): Flow<List<Category>> = categoryRepository.getCategoriesByMangaIdAsFlow(mangaId)

    /** Every category in order. */
    public suspend fun await(): List<Category> = categoryRepository.getAll()

    /** The manga's categories in order. */
    public suspend fun await(mangaId: Long): List<Category> = categoryRepository.getCategoriesByMangaId(mangaId)
}
