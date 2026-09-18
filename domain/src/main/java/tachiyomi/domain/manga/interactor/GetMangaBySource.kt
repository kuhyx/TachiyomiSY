package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.MangaRepository

/** Lists every manga row of one source. */
public class GetMangaBySource(
    private val mangaRepository: MangaRepository,
) {

    /** Every manga of source [sourceId], favourite or not. */
    public suspend fun await(sourceId: Long): List<Manga> = mangaRepository.getMangaBySourceId(sourceId)
}
