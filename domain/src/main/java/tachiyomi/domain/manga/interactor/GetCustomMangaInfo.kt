package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.model.CustomMangaInfo
import tachiyomi.domain.manga.repository.CustomMangaRepository

/** Reads the user's edits to a manga's details. */
public class GetCustomMangaInfo(
    private val customMangaRepository: CustomMangaRepository,
) {

    /** The edits for manga [mangaId], or null when the user made none. */
    public fun get(mangaId: Long): CustomMangaInfo? = customMangaRepository.get(mangaId)
}
