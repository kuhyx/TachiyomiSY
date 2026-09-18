package tachiyomi.domain.source.repository

import tachiyomi.domain.source.model.FeedSavedSearch

/**
 * Persistence of feed entries (saved searches pinned to the feed tab, SY).
 * Reads live in [FeedSavedSearchReadRepository]; the writes are here.
 */
public interface FeedSavedSearchRepository : FeedSavedSearchReadRepository {

    /** Deletes the feed entry [feedSavedSearchId]. */
    public suspend fun delete(feedSavedSearchId: Long)

    /** Inserts [feedSavedSearch] and returns its id, or null when the insert failed. */
    public suspend fun insert(feedSavedSearch: FeedSavedSearch): Long?

    /** Inserts every entry of [feedSavedSearch] in one transaction. */
    public suspend fun insertAll(feedSavedSearch: List<FeedSavedSearch>)
}
