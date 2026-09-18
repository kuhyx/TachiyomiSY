package tachiyomi.domain.manga.model

import tachiyomi.domain.manga.interactor.GetCustomMangaInfo
import uy.kohesive.injekt.injectLazy

/**
 * What the cover fetcher needs to locate and cache a manga's cover.
 *
 * @property mangaId Row id of the manga.
 * @property sourceId Id of the source the cover is fetched from.
 * @property isMangaFavorite Whether the manga is in the library, which is when a custom cover may apply.
 * @property ogUrl Cover url as reported by the source.
 * @property lastModified Epoch millis of the last cover change, for cache keys.
 */
public data class MangaCover(
    val mangaId: Long,
    val sourceId: Long,
    val isMangaFavorite: Boolean,
    // SY -->
    val ogUrl: String?,
    // SY <--
    val lastModified: Long,
) {
    // SY -->
    private val customThumbnailUrl = if (isMangaFavorite) {
        getCustomMangaInfo.get(mangaId)?.thumbnailUrl
    } else {
        null
    }

    /** [ogUrl] unless the user set a custom cover for a favourite. */
    val url: String? = customThumbnailUrl ?: ogUrl

    /** Holds the lazily injected custom-info lookup shared by every cover. */
    public companion object {
        private val getCustomMangaInfo: GetCustomMangaInfo by injectLazy()
    }
    // SY <--
}

/** The [MangaCover] the cover fetcher uses for this manga. */
public fun Manga.asMangaCover(): MangaCover {
    return MangaCover(
        mangaId = id,
        sourceId = source,
        isMangaFavorite = favorite,
        ogUrl = thumbnailUrl,
        lastModified = coverLastModified,
    )
}
