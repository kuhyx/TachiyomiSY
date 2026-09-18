package tachiyomi.domain.manga.repository

import tachiyomi.domain.manga.model.FavoriteEntry
import tachiyomi.domain.manga.model.FavoriteEntryAlternative

/** The local snapshot of E-Hentai favourites the favourites sync diffs against. */
public interface FavoritesEntryRepository {
    /** Deletes every snapshot entry. */
    public suspend fun deleteAll()

    /** Inserts every entry in [favoriteEntries] in one transaction. */
    public suspend fun insertAll(favoriteEntries: List<FavoriteEntry>)

    /** Every snapshot entry. */
    public suspend fun selectAll(): List<FavoriteEntry>

    /** Records [favoriteEntryAlternative] on the snapshot entry it names. */
    public suspend fun addAlternative(favoriteEntryAlternative: FavoriteEntryAlternative)
}
