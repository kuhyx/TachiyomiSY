package tachiyomi.domain.category.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.model.CategoryUpdate

/** Reads and writes the `categories` table and the manga-category links. */
public interface CategoryRepository {

    /** The category with [id], or null if there is none. */
    public suspend fun get(id: Long): Category?

    /** Every category, the built-in one included, in [Category.order]. */
    public suspend fun getAll(): List<Category>

    /** [getAll], re-emitted on every change. */
    public fun getAllAsFlow(): Flow<List<Category>>

    /** The categories the manga is in, in [Category.order]. */
    public suspend fun getCategoriesByMangaId(mangaId: Long): List<Category>

    /** [getCategoriesByMangaId], re-emitted on every change. */
    public fun getCategoriesByMangaIdAsFlow(mangaId: Long): Flow<List<Category>>

    // SY -->

    /** Inserts a new row (the id is ignored) and returns its id, or null if none was reported. */
    public suspend fun insert(category: Category): Long?
    // SY <--

    /** Writes the non-null fields of [update] to its category. */
    public suspend fun updatePartial(update: CategoryUpdate)

    /** [updatePartial] for each item, in one transaction. */
    public suspend fun updatePartial(updates: List<CategoryUpdate>)

    /** Sets every category's flags to [flags]. */
    public suspend fun updateAllFlags(flags: Long?)

    /** Deletes the category; its manga links go with it. */
    public suspend fun delete(categoryId: Long)
}
