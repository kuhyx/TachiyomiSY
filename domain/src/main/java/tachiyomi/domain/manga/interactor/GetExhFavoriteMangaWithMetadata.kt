package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.MangaMetadataRepository

/** Lists the E-Hentai favourites that have search metadata, for the favourites sync. */
public class GetExhFavoriteMangaWithMetadata(
    private val mangaMetadataRepository: MangaMetadataRepository,
) {

    /** Favourites from the E-Hentai sources that have metadata. */
    public suspend fun await(): List<Manga> = mangaMetadataRepository.getExhFavoritesWithMetadata()
}
