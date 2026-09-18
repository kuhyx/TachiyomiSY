package tachiyomi.domain.history.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.history.model.History
import tachiyomi.domain.history.model.HistoryUpdate
import tachiyomi.domain.history.model.HistoryWithRelations

/** Reads and writes of the reading history, one row per chapter. */
public interface HistoryRepository {

    /**
     * One row per manga whose title contains [query] (case-insensitive), carrying the chapter read
     * last, newest first; entries with a zeroed read time are left out. Re-emits on every change.
     */
    public fun getHistory(query: String): Flow<List<HistoryWithRelations>>

    /** The most recently read entry across all manga, or null when nothing was read. */
    public suspend fun getLastHistory(): HistoryWithRelations?

    /** The sum of every row's read duration in milliseconds; 0 without history. */
    public suspend fun getTotalReadDuration(): Long

    /** Every history row of the chapters of manga [mangaId]. */
    public suspend fun getHistoryByMangaId(mangaId: Long): List<History>

    /** Zeroes the read time of row [historyId], hiding it from the history list; the row stays. */
    public suspend fun resetHistory(historyId: Long)

    /** [resetHistory] for every row of manga [mangaId]. */
    public suspend fun resetHistoryByMangaId(mangaId: Long)

    /** Deletes every history row; false when the store failed. */
    public suspend fun deleteAllHistory(): Boolean

    /**
     * Inserts the chapter's row or updates it: the read time is replaced and the session
     * duration is added to the stored total.
     */
    public suspend fun upsertHistory(historyUpdate: HistoryUpdate)

    // SY -->

    /** [upsertHistory] for every entry of [historyUpdate] in one transaction. */
    public suspend fun upsertAllHistory(historyUpdate: List<HistoryUpdate>)
    // SY <--
}
