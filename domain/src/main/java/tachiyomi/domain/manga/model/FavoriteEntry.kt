package tachiyomi.domain.manga.model

import exh.metadata.metadata.EHentaiSearchMetadata

/**
 * A snapshot of one E-Hentai favourite, kept locally to diff against the remote favourites list.
 *
 * @property title Gallery title as reported by the source.
 * @property gid Gallery id.
 * @property token Gallery token.
 * @property otherGid Gallery id of an alternative version of the same gallery, if one was recorded.
 * @property otherToken Gallery token of that alternative.
 * @property category Remote favourite category slot (0-9); -1 when not assigned.
 */
public data class FavoriteEntry(

    val title: String,

    val gid: String,

    val token: String,

    val otherGid: String? = null,

    val otherToken: String? = null,

    val category: Int = -1,
)

/** The gallery url of this favourite on its E-Hentai site. */
public fun FavoriteEntry.getUrl(): String = EHentaiSearchMetadata.idAndTokenToUrl(gid, token)
