package eu.kanade.tachiyomi.ui.library

import eu.kanade.tachiyomi.ui.base.libraryManga
import io.mockk.mockk
import tachiyomi.domain.manga.model.CustomMangaInfo
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.CustomMangaRepository
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

/** User edits for favourites: ids 11..17 have one field each, in the order the reset check reads them; 18 has all. */
internal object EditedInfo : CustomMangaRepository {
    override fun get(mangaId: Long): CustomMangaInfo? = when (mangaId) {
        11L -> CustomMangaInfo(mangaId, title = "Edited")
        12L -> CustomMangaInfo(mangaId, title = null, author = "a")
        13L -> CustomMangaInfo(mangaId, title = null, artist = "a")
        14L -> CustomMangaInfo(mangaId, title = null, thumbnailUrl = "a")
        15L -> CustomMangaInfo(mangaId, title = null, description = "a")
        16L -> CustomMangaInfo(mangaId, title = null, genre = listOf("a"))
        17L -> CustomMangaInfo(mangaId, title = null, status = 1)
        18L -> CustomMangaInfo(
            id = mangaId,
            title = "[G] Real",
            author = "a",
            artist = "a",
            thumbnailUrl = "a",
            description = "a",
            genre = listOf("a"),
            status = 1,
        )
        else -> null
    }

    override fun set(mangaInfo: CustomMangaInfo) = Unit
}
