package tachiyomi.data.category

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import kotlinx.coroutines.flow.Flow
import tachiyomi.data.Database
import tachiyomi.data.awaitList
import tachiyomi.data.awaitOneOrNull
import tachiyomi.data.category.CategoryMapper.mapCategory
import tachiyomi.data.subscribeToList
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.model.CategoryUpdate
import tachiyomi.domain.category.repository.CategoryRepository

/** [CategoryRepository] on the SQLDelight `categories` table. */
public class CategoryRepositoryImpl(
    private val database: Database,
) : CategoryRepository {

    override suspend fun get(id: Long): Category? {
        return database.categoriesQueries
            .getCategory(id)
            .awaitOneOrNull(::mapCategory)
    }

    override suspend fun getAll(): List<Category> {
        return database.categoriesQueries
            .getCategories()
            .awaitList(::mapCategory)
    }

    override fun getAllAsFlow(): Flow<List<Category>> {
        return database.categoriesQueries
            .getCategories()
            .subscribeToList(::mapCategory)
    }

    override suspend fun getCategoriesByMangaId(mangaId: Long): List<Category> {
        return database.categoriesQueries
            .getCategoriesByMangaId(mangaId)
            .awaitList(::mapCategory)
    }

    override fun getCategoriesByMangaIdAsFlow(mangaId: Long): Flow<List<Category>> {
        return database.categoriesQueries
            .getCategoriesByMangaId(mangaId)
            .subscribeToList(::mapCategory)
    }

    // SY -->
    override suspend fun insert(category: Category): Long? {
        return database.categoriesQueries.insert(
            name = category.name,
            order = category.order,
            flags = category.flags,
            version = category.version,
            uid = category.uid,
            last_modified_at = category.lastModifiedAt,
        ).awaitAsOneOrNull()
    }
    // SY <--

    override suspend fun updatePartial(update: CategoryUpdate) {
        database.categoriesQueries.update(
            name = update.name,
            order = update.order,
            flags = update.flags,
            version = update.version,
            uid = update.uid,
            last_modified_at = update.lastModifiedAt,
            isSyncing = null,
            categoryId = update.id,
        )
    }

    override suspend fun updatePartial(updates: List<CategoryUpdate>) {
        database.transaction {
            updates.forEach { updatePartial(it) }
        }
    }

    override suspend fun updateAllFlags(flags: Long?) {
        database.categoriesQueries.updateAllFlags(flags)
    }

    override suspend fun delete(categoryId: Long) {
        database.categoriesQueries.delete(categoryId = categoryId)
    }
}
