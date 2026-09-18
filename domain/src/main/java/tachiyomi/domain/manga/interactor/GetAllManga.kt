package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.MangaRepository

/** Lists every manga row, favourite or not. */
public class GetAllManga(
    private val mangaRepository: MangaRepository,
) {

    /** Every manga row. */
    public suspend fun await(): List<Manga> = mangaRepository.getAll()
}
