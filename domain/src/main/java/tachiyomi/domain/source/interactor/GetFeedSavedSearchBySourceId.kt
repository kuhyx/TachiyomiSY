package tachiyomi.domain.source.interactor

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.source.model.FeedSavedSearch
import tachiyomi.domain.source.repository.FeedSavedSearchRepository

/** Reads the feed entries of one source (SY). */
public class GetFeedSavedSearchBySourceId(
    private val feedSavedSearchRepository: FeedSavedSearchRepository,
) {

    /** Feed entries of source [sourceId]; empty when it has none. */
    public suspend fun await(sourceId: Long): List<FeedSavedSearch> = feedSavedSearchRepository.getBySourceId(sourceId)

    /** [await] as a flow that re-emits on every change. */
    public fun subscribe(sourceId: Long): Flow<List<FeedSavedSearch>> =
        feedSavedSearchRepository.getBySourceIdAsFlow(sourceId)
}
