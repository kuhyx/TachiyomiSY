package mihon.domain.manga.model

import eu.kanade.tachiyomi.source.model.SManga
import tachiyomi.domain.manga.model.Manga

/** A fresh [Manga] row of source [sourceId] carrying this source model's fields; the id is unset. */
public fun SManga.toDomainManga(sourceId: Long): Manga {
    return Manga.create().copy(
        url = url,
        // SY -->
        ogTitle = title,
        ogArtist = artist,
        ogAuthor = author,
        ogDescription = description,
        ogGenre = getGenres(),
        ogStatus = status.toLong(),
        ogThumbnailUrl = thumbnail_url,
        // SY <--
        updateStrategy = update_strategy,
        initialized = initialized,
        memo = memo,
        source = sourceId,
    )
}
