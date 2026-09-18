package tachiyomi.domain.history.interactor

import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.history.model.HistoryUpdate
import tachiyomi.domain.history.repository.HistoryRepository

/** Records reading progress: the read time of a chapter and the time spent on it. */
public class UpsertHistory(
    private val historyRepository: HistoryRepository,
) {

    /**
     * Stores [historyUpdate], creating the chapter's history row or updating its read time and adding
     * the session duration to its total. Store failures are logged and swallowed.
     */
    public suspend fun await(historyUpdate: HistoryUpdate) {
        try {
            historyRepository.upsertHistory(historyUpdate)
        } catch (expected: Exception) {
            // Any failure of the store is logged and reported as the fallback below.
            logcat(LogPriority.ERROR, expected)
        }
    }

    // SY -->

    /** [await] for every entry of [historyUpdate] in one transaction. Store failures are logged and swallowed. */
    public suspend fun awaitAll(historyUpdate: List<HistoryUpdate>) {
        try {
            historyRepository.upsertAllHistory(historyUpdate)
        } catch (expected: Exception) {
            // Any failure of the store is logged and reported as the fallback below.
            logcat(LogPriority.ERROR, expected)
        }
    }
    // SY <--
}
