package tachiyomi.data.history

import app.cash.sqldelight.async.coroutines.awaitAsOne
import kotlinx.coroutines.flow.Flow
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.Database
import tachiyomi.data.awaitList
import tachiyomi.data.awaitOneOrNull
import tachiyomi.data.subscribeToList
import tachiyomi.domain.history.model.History
import tachiyomi.domain.history.model.HistoryUpdate
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.domain.history.repository.HistoryRepository

/** [HistoryRepository] on the SQLDelight `history` table and its view. */
public class HistoryRepositoryImpl(
    private val database: Database,
) : HistoryRepository {

    override fun getHistory(query: String): Flow<List<HistoryWithRelations>> {
        return database.historyViewQueries
            .history(query)
            .subscribeToList(HistoryMapper::mapHistoryWithRelations)
    }

    override suspend fun getLastHistory(): HistoryWithRelations? {
        return database.historyViewQueries
            .getLatestHistory()
            .awaitOneOrNull(HistoryMapper::mapLatestHistory)
    }

    override suspend fun getTotalReadDuration(): Long {
        return database.historyQueries
            .getReadDuration()
            .awaitAsOne()
    }

    override suspend fun getHistoryByMangaId(mangaId: Long): List<History> {
        return database.historyQueries
            .getHistoryByMangaId(mangaId)
            .awaitList(HistoryMapper::mapHistory)
    }

    override suspend fun resetHistory(historyId: Long) {
        try {
            database.historyQueries.resetHistoryById(historyId)
        } catch (expected: Exception) {
            // Any failure of the store is logged and reported as the fallback below.
            logcat(LogPriority.ERROR, throwable = expected)
        }
    }

    override suspend fun resetHistoryByMangaId(mangaId: Long) {
        try {
            database.historyQueries.resetHistoryByMangaId(mangaId)
        } catch (expected: Exception) {
            // Any failure of the store is logged and reported as the fallback below.
            logcat(LogPriority.ERROR, throwable = expected)
        }
    }

    override suspend fun deleteAllHistory(): Boolean {
        return try {
            database.historyQueries.removeAllHistory()
            true
        } catch (expected: Exception) {
            // Any failure of the store is logged and reported as the fallback below.
            logcat(LogPriority.ERROR, throwable = expected)
            false
        }
    }

    override suspend fun upsertHistory(historyUpdate: HistoryUpdate) {
        // SY -->
        partialUpdate(listOf(historyUpdate))
        // SY <--
    }

    // SY -->
    override suspend fun upsertAllHistory(historyUpdate: List<HistoryUpdate>) {
        partialUpdate(historyUpdate)
    }

    private suspend fun partialUpdate(historyUpdates: List<HistoryUpdate>) {
        try {
            database.transaction {
                historyUpdates.forEach { historyUpdate ->
                    database.historyQueries.upsert(
                        chapterId = historyUpdate.chapterId,
                        readAt = historyUpdate.readAt,
                        time_read = historyUpdate.sessionReadDuration,
                    )
                }
            }
        } catch (expected: Exception) {
            // Any failure of the store is logged and reported as the fallback below.
            logcat(LogPriority.ERROR, throwable = expected)
        }
    }
    // SY <--
}
