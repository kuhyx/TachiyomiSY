package tachiyomi.domain.history.interactor

import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.domain.history.repository.HistoryRepository

/**
 * Clears reading history. Single entries are reset (their read time zeroed, so they drop
 * out of the history screen but keep their read duration); [awaitAll] deletes the rows.
 */
public class RemoveHistory(
    private val repository: HistoryRepository,
) {

    /** Deletes every history row; false when the store failed (the failure is logged). */
    public suspend fun awaitAll(): Boolean = repository.deleteAllHistory()

    /** Resets the entry [history] refers to. Store failures are logged and swallowed. */
    public suspend fun await(history: HistoryWithRelations) {
        repository.resetHistory(history.id)
    }

    /** Resets every entry of manga [mangaId]. Store failures are logged and swallowed. */
    public suspend fun await(mangaId: Long) {
        repository.resetHistoryByMangaId(mangaId)
    }

    // SY -->

    /** Resets the entry with row id [historyId]. Store failures are logged and swallowed. */
    public suspend fun awaitById(historyId: Long) {
        repository.resetHistory(historyId)
    }
    // SY <--
}
