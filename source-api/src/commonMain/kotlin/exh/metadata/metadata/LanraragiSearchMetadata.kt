package exh.metadata.metadata

import android.content.Context
import android.net.Uri
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.copy
import kotlinx.serialization.Serializable
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.sy.SYMR

/** Archive metadata from a LANraragi server. */
@Serializable
public class LanraragiSearchMetadata : RaisedSearchMetadata() {
    /** Relative reader URL derived from [arcId]; setting it parses the id back. */
    public var url: String? get() = arcId?.let { "/reader?id=$it" }
        set(a) {
            a?.let {
                arcId = a
            }
        }

    /** Archive id. */
    public var arcId: String? = null

    /** Title. */
    public var title: String? = null

    /** Summary text. */
    public var summary: String? = null

    /** Page count. */
    public var pageCount: Int? = null

    /** Server root the archive was fetched from. */
    public var baseUrl: String? = null

    /** Archive file name on the server. */
    public var filename: String? = null

    /** Archive file extension. */
    public var extension: String? = null

    override fun createMangaInfo(manga: SManga): SManga {
        val key = url

        val cover = if (baseUrl != null && arcId != null) {
            getThumbnailUri(baseUrl!!, arcId!!, 1)
        } else {
            null
        }

        val title = title
        // Set artist (if we can find one)
        val artist = tags.ofNamespace(LANRARAGI_NAMESPACE_ARTIST).let { tags ->
            if (tags.isNotEmpty()) tags.joinToString(transform = { it.name }) else null
        }

        // Copy tags -> genres
        val genres = tagsToGenreString()

        // We default to completed
        val status = SManga.COMPLETED

        return manga.copy(
            url = key ?: manga.url,
            thumbnailUrl = cover ?: manga.thumbnail_url,
            title = title ?: manga.title,
            artist = artist ?: manga.artist,
            author = artist ?: manga.artist,
            genre = genres,
            status = status,
            description = summary ?: manga.description,
        )
    }

    override fun getExtraInfoPairs(context: Context): List<Pair<String, String>> {
        return with(context) {
            listOfNotNull(
                getItem(arcId) { stringResource(SYMR.strings.id) },
                getItem(pageCount) { stringResource(SYMR.strings.page_count) },
                getItem(filename) { stringResource(SYMR.strings.filename) },
                getItem(extension) { stringResource(SYMR.strings.file_extension) },
                getItem(baseUrl) { stringResource(SYMR.strings.base_url) },
            )
        }
    }

    /** Constants and URL helpers. */
    public companion object {
        /** Type of every scraped tag. */
        public const val TAG_TYPE_DEFAULT: Int = 0

        /** Namespace of untyped tags. */
        public const val LANRARAGI_NAMESPACE_OTHER: String = "other"

        /** Namespace of the date-added tag. */
        public const val LANRARAGI_NAMESPACE_DATE_ADDED: String = "date_added"

        /** Namespace of the timestamp tag. */
        public const val LANRARAGI_NAMESPACE_TIMESTAMP: String = "timestamp"

        /** Namespace of artist tags. */
        public const val LANRARAGI_NAMESPACE_ARTIST: String = "artist"

        /** A URI builder for an API [path] on [baseUrl]. */
        public fun getApiUriBuilder(baseUrl: String, path: String): Uri.Builder =
            Uri.parse("$baseUrl$path").buildUpon()

        /** The thumbnail URL of [page] of archive [id] on [baseUrl]. */
        public fun getThumbnailUri(baseUrl: String, id: String, page: Int): String {
            val uri = getApiUriBuilder(baseUrl, "/api/archives/$id/thumbnail")

            if (page > 1) {
                uri.appendQueryParameter("page", page.toString())
                uri.appendQueryParameter("no_fallback", "true")
            }

            return uri.toString()
        }
    }
}
