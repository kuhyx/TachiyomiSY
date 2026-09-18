package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.repository.MangaRepository

/** Removes a manga row from the database. */
public class DeleteMangaById(
    private val mangaRepository: MangaRepository,
) {

    /** Deletes the manga row [id]; failures propagate. */
    public suspend fun await(id: Long) {
        mangaRepository.deleteManga(id)
    }
}
