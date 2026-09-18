package tachiyomi.domain.history.interactor

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.history.model.History
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.domain.history.repository.HistoryRepository

/** Reads of the reading history, per manga or as the searchable history screen list. */
public class GetHistory(
    private val repository: HistoryRepository,
) {

    /** Every history row of the chapters of manga [mangaId]; empty when none was read. */
    public suspend fun await(mangaId: Long): List<History> = repository.getHistoryByMangaId(mangaId)

    /**
     * The history screen list as a flow: one row per manga whose title contains [query], carrying the
     * chapter read last, newest first.
     */
    public fun subscribe(query: String): Flow<List<HistoryWithRelations>> = repository.getHistory(query)
}
