package tachiyomi.domain.source.interactor

import tachiyomi.domain.source.model.SavedSearch
import tachiyomi.domain.source.repository.SavedSearchRepository

/** Looks up one saved search by id (SY). */
public class GetSavedSearchById(
    private val savedSearchRepository: SavedSearchRepository,
) {

    /** The saved search [savedSearchId]; throws when there is none. */
    public suspend fun await(savedSearchId: Long): SavedSearch = savedSearchRepository.getById(savedSearchId)!!

    /** The saved search [savedSearchId], or null when there is none. */
    public suspend fun awaitOrNull(savedSearchId: Long): SavedSearch? = savedSearchRepository.getById(savedSearchId)
}
