package eu.kanade.tachiyomi.ui.base

import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.model.Manga

/** A library entry around a manga with [id]; the counts default to an untouched entry. */
internal fun libraryManga(
    id: Long,
    manga: Manga = Manga.create().copy(id = id, source = 1L),
    categories: List<Long> = emptyList(),
    totalChapters: Long = 0,
    readCount: Long = 0,
): LibraryManga = LibraryManga(
    manga = manga,
    categories = categories,
    totalChapters = totalChapters,
    readCount = readCount,
    bookmarkCount = 0,
    latestUpload = 0,
    chapterFetchedAt = 0,
    lastRead = 0,
)
