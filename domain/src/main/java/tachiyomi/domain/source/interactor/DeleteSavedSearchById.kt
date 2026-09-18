package tachiyomi.domain.source.interactor

import tachiyomi.domain.source.repository.SavedSearchRepository

/** Removes one of the user's saved searches (SY). */
public class DeleteSavedSearchById(
    private val savedSearchRepository: SavedSearchRepository,
) {

    /** Deletes the saved search [savedSearchId]; a store failure propagates. */
    public suspend fun await(savedSearchId: Long) {
        savedSearchRepository.delete(savedSearchId)
    }
}
