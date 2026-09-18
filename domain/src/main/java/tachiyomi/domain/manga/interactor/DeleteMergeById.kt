package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.repository.MangaMergeRepository

/** Removes a single entry from a merged manga. */
public class DeleteMergeById(
    private val mangaMergeRepository: MangaMergeRepository,
) {

    /** Deletes the reference row [id]; failures propagate. */
    public suspend fun await(id: Long) {
        mangaMergeRepository.deleteById(id)
    }
}
