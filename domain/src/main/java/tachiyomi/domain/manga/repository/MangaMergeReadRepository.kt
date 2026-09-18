package tachiyomi.domain.manga.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MergedMangaReference

/** The read half of [MangaMergeRepository]: merged manga and their references. */
public interface MangaMergeReadRepository {

    /** Every manga that is part of a merge. */
    public suspend fun getMergedManga(): List<Manga>

    /** [getMergedManga] as a flow. */
    public suspend fun subscribeMergedManga(): Flow<List<Manga>>

    /** The manga merged into merge [id]. */
    public suspend fun getMergedMangaById(id: Long): List<Manga>

    /** [getMergedMangaById] as a flow. */
    public suspend fun subscribeMergedMangaById(id: Long): Flow<List<Manga>>

    /** The references of merge [id]. */
    public suspend fun getReferencesById(id: Long): List<MergedMangaReference>

    /** [getReferencesById] as a flow. */
    public suspend fun subscribeReferencesById(id: Long): Flow<List<MergedMangaReference>>

    /** The manga of merge [mergeId] whose chapters are downloaded. */
    public suspend fun getMergeMangaForDownloading(mergeId: Long): List<Manga>
}
