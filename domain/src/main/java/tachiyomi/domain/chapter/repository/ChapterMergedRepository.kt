package tachiyomi.domain.chapter.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.chapter.model.Chapter

/** The SY merged-manga reads of [ChapterRepository]: chapters across every source of a merge. */
public interface ChapterMergedRepository {

    /** Chapters of every manga merged into [mangaId]; [applyScanlatorFilter] as in the plain lookup. */
    public suspend fun getMergedChapterByMangaId(mangaId: Long, applyScanlatorFilter: Boolean = false): List<Chapter>

    /** [getMergedChapterByMangaId] as a flow. */
    public suspend fun getMergedChapterByMangaIdFlow(
        mangaId: Long,
        applyScanlatorFilter: Boolean = false,
    ): Flow<List<Chapter>>

    /** Distinct scanlators across the merge [mangaId]. */
    public suspend fun getScanlatorsByMergeId(mangaId: Long): List<String>

    /** [getScanlatorsByMergeId] as a flow. */
    public fun getScanlatorsByMergeIdAsFlow(mangaId: Long): Flow<List<String>>
}
