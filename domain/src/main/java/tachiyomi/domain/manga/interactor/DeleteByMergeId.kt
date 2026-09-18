package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.repository.MangaMergeRepository

/** Removes every reference of a merged manga, dissolving the merge. */
public class DeleteByMergeId(
    private val mangaMergeRepository: MangaMergeRepository,
) {

    /** Deletes every reference of merge [id]; failures propagate. */
    public suspend fun await(id: Long) {
        mangaMergeRepository.deleteByMergeId(id)
    }
}
