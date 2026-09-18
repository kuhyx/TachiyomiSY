package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.model.CustomMangaInfo
import tachiyomi.domain.manga.repository.CustomMangaRepository

/** Saves the user's edits to a favourite's details. */
public class SetCustomMangaInfo(
    private val customMangaRepository: CustomMangaRepository,
) {

    /** Stores [mangaInfo], replacing any earlier edits for the same manga. */
    public fun set(mangaInfo: CustomMangaInfo) {
        customMangaRepository.set(mangaInfo)
    }
}
