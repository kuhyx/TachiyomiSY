package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.source.Source

public interface RandomMangaSource : Source {
    public suspend fun fetchRandomMangaUrl(): String
}
