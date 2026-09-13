package eu.kanade.tachiyomi.source.online

import android.net.Uri
import eu.kanade.tachiyomi.source.Source
import java.net.URI
import java.net.URISyntaxException

public interface UrlImportableSource : Source {
    public val matchingHosts: List<String>

    public fun matchesUri(uri: Uri): Boolean {
        return uri.host.orEmpty().lowercase() in matchingHosts
    }

    public fun mapUrlToChapterUrl(uri: Uri): String? = null

    public suspend fun mapChapterUrlToMangaUrl(uri: Uri): String? = null

    // This method is allowed to block for IO if necessary
    public suspend fun mapUrlToMangaUrl(uri: Uri): String?

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
        } catch (e: URISyntaxException) {
            url
        }
    }

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
        } catch (e: URISyntaxException) {
            url
        }
    }
}
