package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.repository.FavoritesEntryRepository

/** Clears the local E-Hentai favourites snapshot. */
public class DeleteFavoriteEntries(
    private val favoriteEntryRepository: FavoritesEntryRepository,
) {

    /** Deletes every snapshot entry; failures propagate. */
    public suspend fun await() {
        favoriteEntryRepository.deleteAll()
    }
}
