package eu.kanade.presentation.library

import eu.kanade.tachiyomi.ui.library.LibraryItem
import io.mockk.mockk
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.model.Manga

internal fun libraryCategory(id: Long, name: String = "Cat $id"): Category =
    Category(id = id, name = name, order = id, flags = 0L)

/** A library entry titled "Manga [id]"; [unread] drives the continue-reading button. */
internal fun libraryItem(
    id: Long,
    unread: Long = 0L,
    downloads: Int = 0,
    badgeUnread: Long = 0L,
    local: Boolean = false,
    language: String = "",
): LibraryItem {
    val manga = Manga.create().copy(id = id, ogTitle = "Manga $id", favorite = true)
    return LibraryItem(
        libraryManga = LibraryManga(
            manga = manga,
            categories = emptyList(),
            totalChapters = unread,
            readCount = 0L,
            bookmarkCount = 0L,
            latestUpload = 0L,
            chapterFetchedAt = 0L,
            lastRead = 0L,
        ),
        unreadCount = unread,
        sourceManager = mockk(),
        badges = LibraryItem.Badges(
            downloadCount = downloads,
            unreadCount = badgeUnread,
            isLocal = local,
            sourceLanguage = language,
        ),
    )
}
