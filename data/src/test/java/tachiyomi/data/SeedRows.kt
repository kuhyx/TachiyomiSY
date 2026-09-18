package tachiyomi.data

import app.cash.sqldelight.async.coroutines.awaitAsOne
import eu.kanade.tachiyomi.source.model.UpdateStrategy
import kotlinx.serialization.json.JsonObject
import java.util.Date

/** The memo every seeded row carries. */
internal val noMemo: JsonObject = JsonObject(emptyMap())

/** Inserts a `mangas` row and returns its id. */
internal suspend fun Database.seedManga(
    title: String = "Manga",
    source: Long = 1L,
    favorite: Boolean = true,
    genre: List<String>? = null,
    thumbnailUrl: String? = null,
    dateAdded: Long = 0L,
    description: String? = null,
): Long = mangasQueries.insertReturningId(
    source = source, url = "/m/$title", artist = null, author = null, description = description, genre = genre,
    title = title, status = 0L, thumbnailUrl = thumbnailUrl, favorite = favorite, lastUpdate = null,
    nextUpdate = null, initialized = true, viewerFlags = 0L, chapterFlags = 0L, coverLastModified = 0L,
    dateAdded = dateAdded, updateStrategy = UpdateStrategy.ALWAYS_UPDATE, calculateInterval = 0L, version = 0L,
    notes = "", memo = noMemo,
).awaitAsOne()

/** Inserts a `chapters` row for [mangaId] and returns its id. */
internal suspend fun Database.seedChapter(
    mangaId: Long,
    name: String = "Chapter 1",
    url: String = "/c/$mangaId/$name",
    scanlator: String? = null,
    read: Boolean = false,
    bookmark: Boolean = false,
    lastPageRead: Long = 0L,
    dateFetch: Long = 10L,
    dateUpload: Long = 5L,
): Long = chaptersQueries.insertReturningId(
    mangaId = mangaId, url = url, name = name, scanlator = scanlator, read = read, bookmark = bookmark,
    lastPageRead = lastPageRead, chapterNumber = 1.0, sourceOrder = 0L, dateFetch = dateFetch,
    dateUpload = dateUpload, version = 0L, memo = noMemo,
).awaitAsOne()

/** Upserts a `history` row for [chapterId]. */
internal suspend fun Database.seedHistory(chapterId: Long, readAt: Date = Date(1000L), timeRead: Long = 60L) {
    historyQueries.upsert(chapterId = chapterId, readAt = readAt, time_read = timeRead)
}

/** Inserts a `manga_sync` row for [mangaId]. */
internal suspend fun Database.seedTrack(mangaId: Long, trackerId: Long = 1L, remoteId: Long = 100L) {
    manga_syncQueries.insert(
        mangaId = mangaId, syncId = trackerId, remoteId = remoteId, libraryId = null, title = "Tracked $remoteId",
        lastChapterRead = 2.0, totalChapters = 10L, status = 1L, score = 7.5, remoteUrl = "https://t/$remoteId",
        startDate = 0L, finishDate = 0L, private = false,
    )
}

/** Inserts a `merged` row making [mangaId] a part of the merged manga [mergeId]. */
internal suspend fun Database.seedMerged(mergeId: Long, mangaId: Long) {
    mergedQueries.insert(
        infoManga = false, getChapterUpdates = true, chapterSortMode = 0L, chapterPriority = 0L,
        downloadChapters = true, mergeId = mergeId, mergeUrl = "/merged/$mergeId", mangaId = mangaId,
        mangaUrl = "/part/$mangaId", mangaSource = 1L,
    )
}

/** Inserts a `categories` row and returns its id. */
internal suspend fun Database.seedCategory(name: String, order: Long = 0L): Long = categoriesQueries.insert(
    name = name, order = order, flags = 0L, version = 0L, uid = 0L, last_modified_at = 0L,
).awaitAsOne()

/** Puts [mangaId] into [categoryId]. */
internal suspend fun Database.seedMangaCategory(mangaId: Long, categoryId: Long) {
    mangas_categoriesQueries.insert(mangaId = mangaId, categoryId = categoryId)
}

/** Excludes [scanlator] for [mangaId]. */
internal suspend fun Database.seedExcluded(mangaId: Long, scanlator: String) {
    excluded_scanlatorsQueries.insert(mangaId = mangaId, scanlator = scanlator)
}
