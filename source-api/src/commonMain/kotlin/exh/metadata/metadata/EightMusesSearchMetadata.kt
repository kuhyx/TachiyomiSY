package exh.metadata.metadata

import android.content.Context
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.copy
import exh.util.nullIfEmpty
import kotlinx.serialization.Serializable
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR

/** Album metadata scraped from 8muses. */
@Serializable
public class EightMusesSearchMetadata : RaisedSearchMetadata() {
    /** Path segments of the album URL. */
    public var path: List<String> = emptyList()

    /** Title. */
    public var title: String? by titleDelegate(TITLE_TYPE_MAIN)

    /** Cover URL. */
    public var thumbnailUrl: String? = null

    override fun createMangaInfo(manga: SManga): SManga {
        val key = path.joinToString("/", prefix = "/")

        val title = title

        val cover = thumbnailUrl

        val artist = tags.ofNamespace(ARTIST_NAMESPACE).joinToString { it.name }

        val genres = tagsToGenreString()

        val description = "meta"

        return manga.copy(
            url = key,
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
                getItem(title) { stringResource(MR.strings.title) },
                getItem(path.nullIfEmpty(), { it.joinToString("/", prefix = "/") }) {
                    stringResource(SYMR.strings.path)
                },
                getItem(thumbnailUrl) { stringResource(SYMR.strings.thumbnail_url) },
            )
        }
    }

    /** Constants and URL helpers. */
    public companion object {
        private const val TITLE_TYPE_MAIN = 0

        /** Type of every scraped tag. */
        public const val TAG_TYPE_DEFAULT: Int = 0

        /** Namespace of plain tags. */
        public const val TAGS_NAMESPACE: String = "tags"

        /** Namespace of artist tags. */
        public const val ARTIST_NAMESPACE: String = "artist"
    }
}
