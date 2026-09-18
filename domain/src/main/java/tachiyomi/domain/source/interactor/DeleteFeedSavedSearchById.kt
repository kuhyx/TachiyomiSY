package tachiyomi.domain.source.interactor

import tachiyomi.domain.source.repository.FeedSavedSearchRepository

/** Removes one entry from the feed (SY). */
public class DeleteFeedSavedSearchById(
    private val feedSavedSearchRepository: FeedSavedSearchRepository,
) {

    /** Deletes the feed entry [feedSavedSearchId]; a store failure propagates. */
    public suspend fun await(feedSavedSearchId: Long) {
        feedSavedSearchRepository.delete(feedSavedSearchId)
    }
}
