package tachiyomi.domain.source.interactor

import logcat.LogPriority
import logcat.asLog
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.source.model.SavedSearch
import tachiyomi.domain.source.repository.SavedSearchRepository

/** Stores the user's saved searches (SY). */
public class InsertSavedSearch(
    private val savedSearchRepository: SavedSearchRepository,
) {

    /** Inserts [savedSearch] and returns its id; logs and returns null when the store fails. */
    public suspend fun await(savedSearch: SavedSearch): Long? {
        return try {
            savedSearchRepository.insert(savedSearch)
        } catch (expected: Exception) {
            // Any failure of the store is logged and reported as the fallback below.
            logcat(LogPriority.ERROR) { expected.asLog() }
            null
        }
    }

    /** Inserts every entry of [savedSearch] in one transaction; logs and inserts nothing when the store fails. */
    public suspend fun awaitAll(savedSearch: List<SavedSearch>) {
        try {
            savedSearchRepository.insertAll(savedSearch)
        } catch (expected: Exception) {
            // Any failure of the store is logged and reported as the fallback below.
            logcat(LogPriority.ERROR) { expected.asLog() }
        }
    }
}
