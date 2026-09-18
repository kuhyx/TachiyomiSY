package tachiyomi.data.source

import app.cash.sqldelight.async.coroutines.awaitAsOne
import tachiyomi.data.Database
import tachiyomi.domain.source.model.FeedSavedSearch
import tachiyomi.domain.source.repository.FeedSavedSearchReadRepository
import tachiyomi.domain.source.repository.FeedSavedSearchRepository

/** [FeedSavedSearchRepository] on the SQLDelight `feed_saved_search` table (SY); reads are delegated. */
public class FeedSavedSearchRepositoryImpl(
    private val database: Database,
) : FeedSavedSearchRepository,
    FeedSavedSearchReadRepository by FeedSavedSearchReadRepositoryImpl(database) {

    override suspend fun delete(feedSavedSearchId: Long) {
        database.feed_saved_searchQueries
            .deleteById(feedSavedSearchId)
    }

    override suspend fun insert(feedSavedSearch: FeedSavedSearch): Long {
        return database.feed_saved_searchQueries.insertReturningId(
            feedSavedSearch.source,
            feedSavedSearch.savedSearch,
            feedSavedSearch.global,
        ).awaitAsOne()
    }

    override suspend fun insertAll(feedSavedSearch: List<FeedSavedSearch>) {
        return database.transaction {
            feedSavedSearch.forEach {
                database.feed_saved_searchQueries.insert(
                    it.source,
                    it.savedSearch,
                    it.global,
                )
            }
        }
    }
}
