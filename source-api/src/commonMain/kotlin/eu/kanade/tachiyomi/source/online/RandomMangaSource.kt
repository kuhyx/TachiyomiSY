package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.source.Source

/** A source that can pick a random manga. */

public interface RandomMangaSource : Source {
    /** The URL of a random manga. */
    public suspend fun fetchRandomMangaUrl(): String
}
