package tachiyomi.domain.history.interactor

import tachiyomi.domain.history.repository.HistoryRepository

/** The time spent reading across the whole history, for the statistics screen. */
public class GetTotalReadDuration(
    private val repository: HistoryRepository,
) {

    /** The sum of every history row's read duration in milliseconds; 0 when there is no history. */
    public suspend fun await(): Long = repository.getTotalReadDuration()
}
