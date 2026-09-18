package tachiyomi.domain.manga.repository

import tachiyomi.domain.manga.model.MergeMangaSettingsUpdate
import tachiyomi.domain.manga.model.MergedMangaReference

/**
 * Persistence of merged manga (SY). Reads live in [MangaMergeReadRepository];
 * the writes are here.
 */
public interface MangaMergeRepository : MangaMergeReadRepository {

    /** Applies a partial settings [update]; true on success. */
    public suspend fun updateSettings(update: MergeMangaSettingsUpdate): Boolean

    /** Applies every update in [values] in one transaction; true on success. */
    public suspend fun updateAllSettings(values: List<MergeMangaSettingsUpdate>): Boolean

    /** Inserts [reference] and returns its id, or null when the insert failed. */
    public suspend fun insert(reference: MergedMangaReference): Long?

    /** Inserts every reference in [references] in one transaction. */
    public suspend fun insertAll(references: List<MergedMangaReference>)

    /** Deletes the reference [id]. */
    public suspend fun deleteById(id: Long)

    /** Deletes every reference of merge [mergeId]. */
    public suspend fun deleteByMergeId(mergeId: Long)
}
