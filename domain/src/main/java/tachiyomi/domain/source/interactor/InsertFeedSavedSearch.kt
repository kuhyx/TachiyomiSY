package tachiyomi.domain.source.interactor

import logcat.LogPriority
import logcat.asLog
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.source.model.FeedSavedSearch
import tachiyomi.domain.source.repository.FeedSavedSearchRepository

/** Adds entries to the feed (SY). */
public class InsertFeedSavedSearch(
    private val feedSavedSearchRepository: FeedSavedSearchRepository,
) {

    /** Inserts [feedSavedSearch] and returns its id; logs and returns null when the store fails. */
    public suspend fun await(feedSavedSearch: FeedSavedSearch): Long? {
        return try {
            feedSavedSearchRepository.insert(feedSavedSearch)
        } catch (expected: Exception) {
            // Any failure of the store is logged and reported as the fallback below.
            logcat(LogPriority.ERROR) { expected.asLog() }
            null
        }
    }

    /** Inserts every entry of [feedSavedSearch] in one transaction; logs and inserts nothing when the store fails. */
    public suspend fun awaitAll(feedSavedSearch: List<FeedSavedSearch>) {
        try {
            feedSavedSearchRepository.insertAll(feedSavedSearch)
        } catch (expected: Exception) {
            // Any failure of the store is logged and reported as the fallback below.
            logcat(LogPriority.ERROR) { expected.asLog() }
        }
    }
}
