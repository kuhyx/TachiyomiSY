package tachiyomi.domain.manga.model

/**
 * Marks an E-Hentai gallery as an alternative of a favourited one, so the favourites sync treats a
 * newer version of the same gallery as the favourite it replaces.
 *
 * @property otherGid Gallery id of the alternative (the accepted, newer gallery).
 * @property otherToken Gallery token of the alternative.
 * @property gid Gallery id of the favourited entry the alternative is attached to.
 * @property token Gallery token of the favourited entry.
 */
public data class FavoriteEntryAlternative(
    val otherGid: String,
    val otherToken: String,
    val gid: String,
    val token: String,
)
