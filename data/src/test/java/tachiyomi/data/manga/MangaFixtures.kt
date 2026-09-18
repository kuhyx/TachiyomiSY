package tachiyomi.data.manga

import app.cash.sqldelight.async.coroutines.awaitAsOne
import eu.kanade.tachiyomi.source.model.UpdateStrategy
import tachiyomi.data.Database
import tachiyomi.data.Mangas
import tachiyomi.data.noMemo
import tachiyomi.domain.manga.model.Manga

/** Inserts a `mangas` row keyed by [url] (the seed helpers key by title) and returns its id. */
internal suspend fun Database.insertManga(
    url: String,
    favorite: Boolean = false,
    source: Long = 1L,
    title: String = "Title $url",
    nextUpdate: Long? = null,
    status: Long = 0L,
): Long = mangasQueries.insertReturningId(
    source = source, url = url, artist = null, author = null, description = null, genre = null,
    title = title, status = status, thumbnailUrl = null, favorite = favorite, lastUpdate = null,
    nextUpdate = nextUpdate, initialized = false, viewerFlags = 0L, chapterFlags = 0L,
    coverLastModified = 0L, dateAdded = 0L, updateStrategy = UpdateStrategy.ALWAYS_UPDATE,
    calculateInterval = 0L, version = 0L, notes = "", memo = noMemo,
).awaitAsOne()

/** A generated `mangas` row with every column set, for the mappers. */
internal fun mangasRow(id: Long = 1L, lastUpdate: Long? = null, nextUpdate: Long? = null): Mangas = Mangas(
    _id = id, source = 1L, url = "/m/$id", artist = "artist", author = "author", description = "desc",
    genre = listOf("a", "b"), title = "Title", status = 2L, thumbnail_url = "thumb", favorite = false,
    last_update = lastUpdate, next_update = nextUpdate, initialized = true, viewer = 3L, chapter_flags = 4L,
    cover_last_modified = 5L, date_added = 6L, filtered_scanlators = null,
    update_strategy = UpdateStrategy.ONLY_FETCH_ONCE, calculate_interval = 7L, last_modified_at = 8L,
    favorite_modified_at = 9L, version = 10L, is_syncing = 0L, notes = "notes", memo = noMemo,
)

/** A domain [Manga] as a source would report it, for `insertNetworkManga`. */
internal fun networkManga(
    url: String,
    title: String = "Title $url",
    thumbnailUrl: String? = "https://x/$url.png",
    initialized: Boolean = true,
    favorite: Boolean = false,
): Manga = Manga.create().copy(
    source = 1L, url = url, ogTitle = title, ogThumbnailUrl = thumbnailUrl, initialized = initialized,
    favorite = favorite, ogAuthor = "author", ogArtist = "artist", ogDescription = "desc",
    ogGenre = listOf("a", "b"), ogStatus = 2L, lastUpdate = 11L, nextUpdate = 12L, fetchInterval = 3,
)
