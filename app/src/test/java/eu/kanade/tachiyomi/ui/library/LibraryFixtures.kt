package eu.kanade.tachiyomi.ui.library

import eu.kanade.tachiyomi.ui.base.libraryManga
import io.mockk.mockk
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager

/** A library item around [manga]; badges and counts are the unfiltered defaults. */
internal fun libraryItem(
    manga: Manga,
    categories: List<Long> = emptyList(),
    totalChapters: Long = 0,
    readCount: Long = 0,
    downloadCount: Int = 0,
    sourceManager: SourceManager = mockk(relaxed = true),
): LibraryItem = LibraryItem(
    libraryManga = libraryManga(
        id = manga.id,
        manga = manga,
        categories = categories,
        totalChapters = totalChapters,
        readCount = readCount,
    ),
    downloadCount = downloadCount,
    sourceManager = sourceManager,
    badges = LibraryItem.Badges(downloadCount = 0, unreadCount = 0, isLocal = false, sourceLanguage = ""),
)

internal fun manga(id: Long, title: String = "Title $id"): Manga = Manga.create().copy(id = id, ogTitle = title)
