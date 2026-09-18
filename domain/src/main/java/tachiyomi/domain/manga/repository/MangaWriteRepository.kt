package tachiyomi.domain.manga.repository

import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaUpdate

/** The write half of [MangaRepository]. */
public interface MangaWriteRepository {

    /** Clears every manga's viewer flags; true on success. */
    public suspend fun resetViewerFlags(): Boolean

    /** Replaces the categories of [mangaId] with [categoryIds]. */
    public suspend fun setMangaCategories(mangaId: Long, categoryIds: List<Long>)

    /** Applies a partial [update]; true on success. */
    public suspend fun update(update: MangaUpdate): Boolean

    /** Applies every update in [mangaUpdates] in one transaction; true on success. */
    public suspend fun updateAll(mangaUpdates: List<MangaUpdate>): Boolean

    /** Inserts [manga] fetched from a source and returns them with their new ids. */
    public suspend fun insertNetworkManga(manga: List<Manga>): List<Manga>

    // SY -->

    /** Deletes the manga row [mangaId]. */
    public suspend fun deleteManga(mangaId: Long)
    // SY <--
}
