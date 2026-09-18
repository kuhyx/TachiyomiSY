package tachiyomi.domain.source.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.source.model.FeedSavedSearch
import tachiyomi.domain.source.model.SavedSearch

/** The read half of [FeedSavedSearchRepository]: the global feed and each source's feed. */
public interface FeedSavedSearchReadRepository {

    /** Entries of the global feed. */
    public suspend fun getGlobal(): List<FeedSavedSearch>

    /** [getGlobal] as a flow. */
    public fun getGlobalAsFlow(): Flow<List<FeedSavedSearch>>

    /** Saved searches backing the global feed. */
    public suspend fun getGlobalFeedSavedSearch(): List<SavedSearch>

    /** Number of global feed entries. */
    public suspend fun countGlobal(): Long

    /** Feed entries of source [sourceId]. */
    public suspend fun getBySourceId(sourceId: Long): List<FeedSavedSearch>

    /** [getBySourceId] as a flow. */
    public fun getBySourceIdAsFlow(sourceId: Long): Flow<List<FeedSavedSearch>>

    /** Saved searches backing the feed of source [sourceId]. */
    public suspend fun getBySourceIdFeedSavedSearch(sourceId: Long): List<SavedSearch>

    /** Number of feed entries of source [sourceId]. */
    public suspend fun countBySourceId(sourceId: Long): Long
}
