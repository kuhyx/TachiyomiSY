package tachiyomi.domain.source.interactor

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.source.model.FeedSavedSearch
import tachiyomi.domain.source.repository.FeedSavedSearchRepository

/** Reads the entries of the global feed (SY). */
public class GetFeedSavedSearchGlobal(
    private val feedSavedSearchRepository: FeedSavedSearchRepository,
) {

    /** Entries of the global feed; empty when there are none. */
    public suspend fun await(): List<FeedSavedSearch> = feedSavedSearchRepository.getGlobal()

    /** [await] as a flow that re-emits on every change. */
    public fun subscribe(): Flow<List<FeedSavedSearch>> = feedSavedSearchRepository.getGlobalAsFlow()
}
