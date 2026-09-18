package tachiyomi.domain.source.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.source.model.SavedSearch

/** Persistence of the user's saved searches (SY). */
public interface SavedSearchRepository {

    /** The saved search with id [savedSearchId], or null when there is none. */
    public suspend fun getById(savedSearchId: Long): SavedSearch?

    /** Saved searches of source [sourceId]. */
    public suspend fun getBySourceId(sourceId: Long): List<SavedSearch>

    /** [getBySourceId] as a flow. */
    public fun getBySourceIdAsFlow(sourceId: Long): Flow<List<SavedSearch>>

    /** Deletes the saved search [savedSearchId]. */
    public suspend fun delete(savedSearchId: Long)

    /** Inserts [savedSearch] and returns its id, or null when the insert failed. */
    public suspend fun insert(savedSearch: SavedSearch): Long?

    /** Inserts every entry of [savedSearch] in one transaction. */
    public suspend fun insertAll(savedSearch: List<SavedSearch>)
}
