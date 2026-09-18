package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.model.FavoriteEntry
import tachiyomi.domain.manga.repository.FavoritesEntryRepository

/** Writes entries into the local E-Hentai favourites snapshot. */
public class InsertFavoriteEntries(
    private val favoriteEntryRepository: FavoritesEntryRepository,
) {

    /** Inserts every entry in [entries] in one transaction; failures propagate. */
    public suspend fun await(entries: List<FavoriteEntry>) {
        favoriteEntryRepository.insertAll(entries)
    }
}
