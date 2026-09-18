package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.MangaRepository

/** Looks up a manga by its url within one source. */
public class GetMangaByUrlAndSourceId(
    private val mangaRepository: MangaRepository,
) {
    /** The manga at [url] in source [sourceId], or null. */
    public suspend fun await(url: String, sourceId: Long): Manga? =
        mangaRepository.getMangaByUrlAndSourceId(url, sourceId)
}
