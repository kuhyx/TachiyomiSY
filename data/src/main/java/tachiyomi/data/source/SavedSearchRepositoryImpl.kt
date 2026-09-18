package tachiyomi.data.source

import app.cash.sqldelight.async.coroutines.awaitAsOne
import kotlinx.coroutines.flow.Flow
import tachiyomi.data.Database
import tachiyomi.data.awaitList
import tachiyomi.data.awaitOneOrNull
import tachiyomi.data.subscribeToList
import tachiyomi.domain.source.model.SavedSearch
import tachiyomi.domain.source.repository.SavedSearchRepository

/** [SavedSearchRepository] on the SQLDelight `saved_search` table (SY). */
public class SavedSearchRepositoryImpl(
    private val database: Database,
) : SavedSearchRepository {

    override suspend fun getById(savedSearchId: Long): SavedSearch? {
        return database.saved_searchQueries
            .selectById(savedSearchId)
            .awaitOneOrNull(SavedSearchMapper::map)
    }

    override suspend fun getBySourceId(sourceId: Long): List<SavedSearch> {
        return database.saved_searchQueries
            .selectBySource(sourceId)
            .awaitList(SavedSearchMapper::map)
    }

    override fun getBySourceIdAsFlow(sourceId: Long): Flow<List<SavedSearch>> {
        return database.saved_searchQueries
            .selectBySource(sourceId)
            .subscribeToList(SavedSearchMapper::map)
    }

    override suspend fun delete(savedSearchId: Long) {
        database.saved_searchQueries
            .deleteById(savedSearchId)
    }

    override suspend fun insert(savedSearch: SavedSearch): Long {
        return database.saved_searchQueries.insertReturningId(
            savedSearch.source,
            savedSearch.name,
            savedSearch.query,
            savedSearch.filtersJson,
        ).awaitAsOne()
    }

    override suspend fun insertAll(savedSearch: List<SavedSearch>) {
        database.transaction {
            savedSearch.forEach {
                database.saved_searchQueries.insert(
                    it.source,
                    it.name,
                    it.query,
                    it.filtersJson,
                )
            }
        }
    }
}
