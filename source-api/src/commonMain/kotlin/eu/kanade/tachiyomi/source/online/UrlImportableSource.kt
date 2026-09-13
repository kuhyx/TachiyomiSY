package eu.kanade.tachiyomi.source.online

import android.net.Uri
import eu.kanade.tachiyomi.source.Source
import java.net.URI
import java.net.URISyntaxException

/** A source that can turn a shared web link into one of its manga or chapters. */

public interface UrlImportableSource : Source {
    /** Lower-case hosts this source claims. */
    public val matchingHosts: List<String>

    /** True when [uri] is on one of [matchingHosts]. */

    public fun matchesUri(uri: Uri): Boolean = uri.host.orEmpty().lowercase() in matchingHosts

    /** The chapter URL for [uri], or null when it is not a chapter link. */

    public fun mapUrlToChapterUrl(uri: Uri): String? = null

    /** The manga URL that owns the chapter at [uri], or null. */

    public suspend fun mapChapterUrlToMangaUrl(uri: Uri): String? = null

    // This method is allowed to block for IO if necessary

    /** The manga URL for [uri], or null when it is not a manga link. */
    public suspend fun mapUrlToMangaUrl(uri: Uri): String?

    /** [url] without scheme and host, as stored for a manga. */

    public fun cleanMangaUrl(url: String): String {
        return try {
            val uri = URI(url)
            var out = uri.path
            if (uri.query != null) {
                out += "?" + uri.query
            }
            if (uri.fragment != null) {
                out += "#" + uri.fragment
            }
            out
        } catch (_: URISyntaxException) {
            url
        }
    }

    /** [url] without scheme and host, as stored for a chapter. */

    public fun cleanChapterUrl(url: String): String {
        return try {
            val uri = URI(url)
            var out = uri.path
            if (uri.query != null) {
                out += "?" + uri.query
            }
            if (uri.fragment != null) {
                out += "#" + uri.fragment
            }
            out
        } catch (_: URISyntaxException) {
            url
        }
    }
}
