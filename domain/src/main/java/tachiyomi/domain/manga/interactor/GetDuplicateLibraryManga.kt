package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaWithChapterCount
import tachiyomi.domain.manga.repository.MangaRepository

/** Finds library entries that share a title with a manga about to be added. */
public class GetDuplicateLibraryManga(
    private val mangaRepository: MangaRepository,
) {

    /** Library entries whose title matches [manga]'s, case-insensitively, excluding [manga] itself. */
    public suspend operator fun invoke(manga: Manga): List<MangaWithChapterCount> =
        mangaRepository.getDuplicateLibraryManga(manga.id, manga.title.lowercase())
}
