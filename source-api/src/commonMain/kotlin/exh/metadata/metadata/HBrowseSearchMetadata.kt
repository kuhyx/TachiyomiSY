package exh.metadata.metadata

import android.content.Context
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.copy
import kotlinx.serialization.Serializable
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR

/** Gallery metadata scraped from HBrowse. */
@Serializable
public class HBrowseSearchMetadata : RaisedSearchMetadata() {
    /** Gallery id. */
    public var hbId: Long? = null

    /** Relative gallery URL. */
    public var hbUrl: String? = null

    /** Cover URL. */
    public var thumbnail: String? = null

    /** Title. */
    public var title: String? by titleDelegate(TITLE_TYPE_MAIN)

    // Length in pages

    /** Page count. */
    public var length: Int? = null

    override fun createMangaInfo(manga: SManga): SManga {
        val key = hbUrl

        val title = title

        // Guess thumbnail URL if manga does not have thumbnail URL
        val cover = if (manga.thumbnail_url.isNullOrBlank()) {
            guessThumbnailUrl(hbId.toString())
        } else {
            null
        }

        val artist = tags.ofNamespace(ARTIST_NAMESPACE).joinToString { it.name }

        val genres = tagsToGenreString()

        val description = "meta"

        return manga.copy(
            url = key ?: manga.url,
            title = title ?: manga.title,
            thumbnail_url = cover ?: manga.thumbnail_url,
            artist = artist,
            genre = genres,
            description = description,
        )
    }

    override fun getExtraInfoPairs(context: Context): List<Pair<String, String>> {
        return with(context) {
            listOfNotNull(
                getItem(hbId) { stringResource(SYMR.strings.id) },
                getItem(hbUrl) { stringResource(SYMR.strings.url) },
                getItem(thumbnail) { stringResource(SYMR.strings.thumbnail_url) },
                getItem(title) { stringResource(MR.strings.title) },
                getItem(length) { stringResource(SYMR.strings.page_count) },
            )
        }
    }

    /** Constants and URL helpers. */
    public companion object {
        /** Site root. */
        public const val BASE_URL: String = "https://www.hbrowse.com"

        private const val TITLE_TYPE_MAIN = 0

        /** Type of every scraped tag. */
        public const val TAG_TYPE_DEFAULT: Int = 0

        /** Namespace of artist tags. */
        public const val ARTIST_NAMESPACE: String = "artist"

        /** The thumbnail URL the site usually serves for gallery [hbid]. */
        public fun guessThumbnailUrl(hbid: String): String = "$BASE_URL/thumbnails/${hbid}_1.jpg#guessed"
    }
}
