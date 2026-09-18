package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.repository.MangaMetadataRepository

/** Lists which favourites already have search metadata stored. */
public class GetIdsOfFavoriteMangaWithMetadata(
    private val mangaMetadataRepository: MangaMetadataRepository,
) {

    /** Ids of every favourite that has metadata. */
    public suspend fun await(): List<Long> = mangaMetadataRepository.getFavoriteIdsWithMetadata()
}
