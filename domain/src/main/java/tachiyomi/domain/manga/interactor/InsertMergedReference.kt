package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.model.MergedMangaReference
import tachiyomi.domain.manga.repository.MangaMergeRepository

/** Adds entries to a merged manga. */
public class InsertMergedReference(
    private val mangaMergedRepository: MangaMergeRepository,
) {

    /** Inserts [reference] and returns its id, or null when the insert failed. */
    public suspend fun await(reference: MergedMangaReference): Long? = mangaMergedRepository.insert(reference)

    /** Inserts every reference in [references] in one transaction; failures propagate. */
    public suspend fun awaitAll(references: List<MergedMangaReference>) {
        mangaMergedRepository.insertAll(references)
    }
}
