package tachiyomi.domain.source.interactor

import tachiyomi.domain.source.repository.FeedSavedSearchRepository

/** Counts the feed entries of one source (SY). */
public class CountFeedSavedSearchBySourceId(
    private val feedSavedSearchRepository: FeedSavedSearchRepository,
) {

    /** Number of feed entries of source [sourceId]. */
    public suspend fun await(sourceId: Long): Long = feedSavedSearchRepository.countBySourceId(sourceId)
}
