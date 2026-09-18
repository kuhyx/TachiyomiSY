package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.model.FavoriteEntry
import tachiyomi.domain.manga.repository.FavoritesEntryRepository

/** Reads the local E-Hentai favourites snapshot. */
public class GetFavoriteEntries(
    private val favoriteEntryRepository: FavoritesEntryRepository,
) {

    /** Every snapshot entry. */
    public suspend fun await(): List<FavoriteEntry> = favoriteEntryRepository.selectAll()
}
