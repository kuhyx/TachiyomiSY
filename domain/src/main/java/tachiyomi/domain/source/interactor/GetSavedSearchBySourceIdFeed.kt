package tachiyomi.domain.source.interactor

import tachiyomi.domain.source.model.SavedSearch
import tachiyomi.domain.source.repository.FeedSavedSearchRepository

/** Reads the saved searches behind one source's feed entries (SY). */
public class GetSavedSearchBySourceIdFeed(
    private val feedSavedSearchRepository: FeedSavedSearchRepository,
) {

    /** Saved searches referenced by the feed of source [sourceId]; empty when it has none. */
    public suspend fun await(sourceId: Long): List<SavedSearch> =
        feedSavedSearchRepository.getBySourceIdFeedSavedSearch(sourceId)
}
