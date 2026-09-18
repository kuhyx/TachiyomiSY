package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.model.FavoriteEntryAlternative
import tachiyomi.domain.manga.repository.FavoritesEntryRepository

/** Records that a newer E-Hentai gallery replaces a favourited one in the favourites snapshot. */
public class InsertFavoriteEntryAlternative(
    private val favoriteEntryRepository: FavoritesEntryRepository,
) {

    /** Attaches [entry] to the snapshot entry it names; failures propagate. */
    public suspend fun await(entry: FavoriteEntryAlternative) {
        favoriteEntryRepository.addAlternative(entry)
    }
}
