package tachiyomi.domain.source.interactor

import tachiyomi.domain.source.model.SavedSearch
import tachiyomi.domain.source.repository.FeedSavedSearchRepository

/** Reads the saved searches behind the global feed's entries (SY). */
public class GetSavedSearchGlobalFeed(
    private val feedSavedSearchRepository: FeedSavedSearchRepository,
) {

    /** Saved searches referenced by the global feed; empty when there are none. */
    public suspend fun await(): List<SavedSearch> = feedSavedSearchRepository.getGlobalFeedSavedSearch()
}
