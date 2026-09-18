package tachiyomi.domain.chapter.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.chapter.model.Chapter

/** Chapter lookups by manga, id or url; the read half of [ChapterRepository]. */
public interface ChapterQueryRepository {

    /** Chapters of [mangaId]; [applyScanlatorFilter] drops the manga's excluded scanlators. */
    public suspend fun getChapterByMangaId(mangaId: Long, applyScanlatorFilter: Boolean = false): List<Chapter>

    /** [getChapterByMangaId] as a flow. */
    public suspend fun getChapterByMangaIdAsFlow(
        mangaId: Long,
        applyScanlatorFilter: Boolean = false,
    ): Flow<List<Chapter>>

    /** Distinct scanlators of [mangaId]'s chapters. */
    public suspend fun getScanlatorsByMangaId(mangaId: Long): List<String>

    /** [getScanlatorsByMangaId] as a flow. */
    public fun getScanlatorsByMangaIdAsFlow(mangaId: Long): Flow<List<String>>

    /** Bookmarked chapters of [mangaId]. */
    public suspend fun getBookmarkedChaptersByMangaId(mangaId: Long): List<Chapter>

    /** The chapter with [id], or null. */
    public suspend fun getChapterById(id: Long): Chapter?

    /** The chapter at [url] of [mangaId], or null. */
    public suspend fun getChapterByUrlAndMangaId(url: String, mangaId: Long): Chapter?

    // SY -->

    /** Every chapter at [url], across manga. */
    public suspend fun getChapterByUrl(url: String): List<Chapter>
    // SY <--
}
