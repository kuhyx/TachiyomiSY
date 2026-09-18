package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.MangaRepository

/** Turns manga fetched from a source into database rows, reusing rows that already exist. */
public class NetworkToLocalManga(
    private val mangaRepository: MangaRepository,
) {

    /** The local row for [manga], inserted if it was not stored yet. */
    public suspend operator fun invoke(manga: Manga): Manga = invoke(listOf(manga)).single()

    /** The local rows for every manga in [manga], in the same order, inserting the ones not stored yet. */
    public suspend operator fun invoke(manga: List<Manga>): List<Manga> = mangaRepository.insertNetworkManga(manga)
}
