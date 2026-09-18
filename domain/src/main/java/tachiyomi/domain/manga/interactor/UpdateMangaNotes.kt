package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.model.MangaUpdate
import tachiyomi.domain.manga.repository.MangaRepository

/** Saves the user's free-text notes on a manga. */
public class UpdateMangaNotes(
    private val mangaRepository: MangaRepository,
) {

    /** Replaces the notes of [mangaId] with [notes]; true on success. */
    public suspend operator fun invoke(mangaId: Long, notes: String): Boolean {
        return mangaRepository.update(
            MangaUpdate(
                id = mangaId,
                notes = notes,
            ),
        )
    }
}
