package tachiyomi.domain.manga.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaWithChapterCount

/** Lookups of single manga and source-scoped lists; the read half of [MangaRepository]. */
public interface MangaQueryRepository {

    /** The manga with [id]; throws when there is none. */
    public suspend fun getMangaById(id: Long): Manga

    /** [getMangaById] as a flow that re-emits on every change. */
    public suspend fun getMangaByIdAsFlow(id: Long): Flow<Manga>

    /** The manga at [url] in source [sourceId], or null. */
    public suspend fun getMangaByUrlAndSourceId(url: String, sourceId: Long): Manga?

    /** [getMangaByUrlAndSourceId] as a flow. */
    public fun getMangaByUrlAndSourceIdAsFlow(url: String, sourceId: Long): Flow<Manga?>

    /** Favourites of source [sourceId], as a flow. */
    public fun getFavoritesBySourceId(sourceId: Long): Flow<List<Manga>>

    /** Library entries whose title matches [title], excluding [id] itself. */
    public suspend fun getDuplicateLibraryManga(id: Long, title: String): List<MangaWithChapterCount>

    /** Library entries in [statuses] expecting a new chapter, as a flow. */
    public suspend fun getUpcomingManga(statuses: Set<Long>): Flow<List<Manga>>

    // SY -->

    /** Every manga of source [sourceId], favourite or not. */
    public suspend fun getMangaBySourceId(sourceId: Long): List<Manga>

    /** Every manga row. */
    public suspend fun getAll(): List<Manga>
    // SY <--
}
