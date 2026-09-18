package tachiyomi.domain.source.interactor

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.source.model.SavedSearch
import tachiyomi.domain.source.repository.SavedSearchRepository

/** Reads the saved searches of one source (SY). */
public class GetSavedSearchBySourceId(
    private val savedSearchRepository: SavedSearchRepository,
) {

    /** Saved searches of source [sourceId]; empty when it has none. */
    public suspend fun await(sourceId: Long): List<SavedSearch> = savedSearchRepository.getBySourceId(sourceId)

    /** [await] as a flow that re-emits on every change. */
    public fun subscribe(sourceId: Long): Flow<List<SavedSearch>> = savedSearchRepository.getBySourceIdAsFlow(sourceId)
}
