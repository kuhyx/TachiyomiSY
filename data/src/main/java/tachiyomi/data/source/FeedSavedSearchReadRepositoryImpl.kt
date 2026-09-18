package tachiyomi.data.source

import app.cash.sqldelight.async.coroutines.awaitAsOne
import kotlinx.coroutines.flow.Flow
import tachiyomi.data.Database
import tachiyomi.data.awaitList
import tachiyomi.data.subscribeToList
import tachiyomi.domain.source.model.FeedSavedSearch
import tachiyomi.domain.source.model.SavedSearch
import tachiyomi.domain.source.repository.FeedSavedSearchReadRepository

/** [FeedSavedSearchReadRepository] on the SQLDelight `feed_saved_search` table (SY). */
internal class FeedSavedSearchReadRepositoryImpl(
    private val database: Database,
) : FeedSavedSearchReadRepository {

    override suspend fun getGlobal(): List<FeedSavedSearch> {
        return database.feed_saved_searchQueries
            .selectAllGlobal()
            .awaitList(FeedSavedSearchMapper::map)
    }

    override fun getGlobalAsFlow(): Flow<List<FeedSavedSearch>> {
        return database.feed_saved_searchQueries
            .selectAllGlobal()
            .subscribeToList(FeedSavedSearchMapper::map)
    }

    override suspend fun getGlobalFeedSavedSearch(): List<SavedSearch> {
        return database.feed_saved_searchQueries
            .selectGlobalFeedSavedSearch()
            .awaitList(SavedSearchMapper::map)
    }

    override suspend fun countGlobal(): Long {
        return database.feed_saved_searchQueries
            .countGlobal()
            .awaitAsOne()
    }

    override suspend fun getBySourceId(sourceId: Long): List<FeedSavedSearch> {
        return database.feed_saved_searchQueries
            .selectBySource(sourceId)
            .awaitList(FeedSavedSearchMapper::map)
    }

    override fun getBySourceIdAsFlow(sourceId: Long): Flow<List<FeedSavedSearch>> {
        return database.feed_saved_searchQueries
            .selectBySource(sourceId)
            .subscribeToList(FeedSavedSearchMapper::map)
    }

    override suspend fun getBySourceIdFeedSavedSearch(sourceId: Long): List<SavedSearch> {
        return database.feed_saved_searchQueries
            .selectSourceFeedSavedSearch(sourceId)
            .awaitList(SavedSearchMapper::map)
    }

    override suspend fun countBySourceId(sourceId: Long): Long {
        return database.feed_saved_searchQueries
            .countSourceFeedSavedSearch(sourceId)
            .awaitAsOne()
    }
}
