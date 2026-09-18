package tachiyomi.domain.manga.repository

import tachiyomi.domain.manga.model.CustomMangaInfo

/** The store of the user's per-manga detail edits ([CustomMangaInfo]). */
public interface CustomMangaRepository {

    /** The edits for [mangaId], or null when the user made none. */
    public fun get(mangaId: Long): CustomMangaInfo?

    /** Saves [mangaInfo], replacing any earlier edits for the same manga. */
    public fun set(mangaInfo: CustomMangaInfo)
}
