package tachiyomi.domain.source.interactor

import tachiyomi.domain.source.repository.FeedSavedSearchRepository

/** Counts the entries of the global feed (SY). */
public class CountFeedSavedSearchGlobal(
    private val feedSavedSearchRepository: FeedSavedSearchRepository,
) {

    /** Number of global feed entries. */
    public suspend fun await(): Long = feedSavedSearchRepository.countGlobal()
}
