package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga

/**
 * A source that may handle opening an SManga or SChapter for a given URI.
 *
 * @since extensions-lib 1.5
 */
public interface ResolvableSource : Source {

    /**
     * Returns what the given URI may open.
     * Returns [UriType.Unknown] if the source is not able to resolve the URI.
     *
     * @since extensions-lib 1.5
     */
    public fun getUriType(uri: String): UriType

    /**
     * Called if [getUriType] is [UriType.Manga].
     * Returns the corresponding SManga, if possible.
     *
     * @since extensions-lib 1.5
     */
    public suspend fun getManga(uri: String): SManga?

    /**
     * Called if [getUriType] is [UriType.Chapter].
     * Returns the corresponding SChapter, if possible.
     *
     * @since extensions-lib 1.5
     */
    public suspend fun getChapter(uri: String): SChapter?
}

public sealed interface UriType {
    public data object Manga : UriType
    public data object Chapter : UriType
    public data object Unknown : UriType
}
