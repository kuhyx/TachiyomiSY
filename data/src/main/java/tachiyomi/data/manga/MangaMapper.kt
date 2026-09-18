package tachiyomi.data.manga

import tachiyomi.data.GetDuplicateLibraryManga
import tachiyomi.data.GetMangasWithFavoriteTimestamp
import tachiyomi.data.Mangas
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaWithChapterCount
import tachiyomi.view.LibraryView

/** Domain models from the generated `mangas` rows and the library view rows. */
public object MangaMapper {

    /** The [Manga] of a `mangas` row. */
    public fun mapManga(row: Mangas): Manga = Manga(
        id = row._id,
        source = row.source,
        favorite = row.favorite,
        lastUpdate = row.last_update ?: 0,
        nextUpdate = row.next_update ?: 0,
        fetchInterval = row.calculate_interval.toInt(),
        dateAdded = row.date_added,
        viewerFlags = row.viewer,
        chapterFlags = row.chapter_flags,
        coverLastModified = row.cover_last_modified,
        url = row.url,
        // SY -->
        ogTitle = row.title,
        ogArtist = row.artist,
        ogAuthor = row.author,
        ogThumbnailUrl = row.thumbnail_url,
        ogDescription = row.description,
        ogGenre = row.genre,
        ogStatus = row.status,
        // SY <--
        updateStrategy = row.update_strategy,
        initialized = row.initialized,
        lastModifiedAt = row.last_modified_at,
        favoriteModifiedAt = row.favorite_modified_at,
        version = row.version,
        notes = row.notes,
        memo = row.memo,
    )

    /** The [Manga] of a favourite-timestamp row (a `mangas` row whose `favorite_modified_at` is set). */
    public fun mapManga(row: GetMangasWithFavoriteTimestamp): Manga = Manga(
        id = row._id,
        source = row.source,
        favorite = row.favorite,
        lastUpdate = row.last_update ?: 0,
        nextUpdate = row.next_update ?: 0,
        fetchInterval = row.calculate_interval.toInt(),
        dateAdded = row.date_added,
        viewerFlags = row.viewer,
        chapterFlags = row.chapter_flags,
        coverLastModified = row.cover_last_modified,
        url = row.url,
        ogTitle = row.title,
        ogArtist = row.artist,
        ogAuthor = row.author,
        ogThumbnailUrl = row.thumbnail_url,
        ogDescription = row.description,
        ogGenre = row.genre,
        ogStatus = row.status,
        updateStrategy = row.update_strategy,
        initialized = row.initialized,
        lastModifiedAt = row.last_modified_at,
        favoriteModifiedAt = row.favorite_modified_at,
        version = row.version,
        notes = row.notes,
        memo = row.memo,
    )

    /** The [LibraryManga] of a library view row (a manga with its chapter counts and categories). */
    public fun mapLibraryManga(row: LibraryView): LibraryManga = LibraryManga(
        manga = Manga(
            id = row._id,
            source = row.source,
            favorite = row.favorite,
            lastUpdate = row.last_update ?: 0,
            nextUpdate = row.next_update ?: 0,
            fetchInterval = row.calculate_interval.toInt(),
            dateAdded = row.date_added,
            viewerFlags = row.viewer,
            chapterFlags = row.chapter_flags,
            coverLastModified = row.cover_last_modified,
            url = row.url,
            ogTitle = row.title,
            ogArtist = row.artist,
            ogAuthor = row.author,
            ogThumbnailUrl = row.thumbnail_url,
            ogDescription = row.description,
            ogGenre = row.genre,
            ogStatus = row.status,
            updateStrategy = row.update_strategy,
            initialized = row.initialized,
            lastModifiedAt = row.last_modified_at,
            favoriteModifiedAt = row.favorite_modified_at,
            version = row.version,
            notes = row.notes,
            memo = row.memo,
        ),
        categories = row.categories.split(",").map { it.toLong() },
        totalChapters = row.totalCount,
        readCount = row.readCount.toLong(),
        bookmarkCount = row.bookmarkCount.toLong(),
        latestUpload = row.latestUpload,
        chapterFetchedAt = row.chapterFetchedAt,
        lastRead = row.lastRead,
    )

    /** The [MangaWithChapterCount] of a duplicate-search row. */
    public fun mapMangaWithChapterCount(row: GetDuplicateLibraryManga): MangaWithChapterCount = MangaWithChapterCount(
        manga = Manga(
            id = row._id,
            source = row.source,
            favorite = row.favorite,
            lastUpdate = row.last_update ?: 0,
            nextUpdate = row.next_update ?: 0,
            fetchInterval = row.calculate_interval.toInt(),
            dateAdded = row.date_added,
            viewerFlags = row.viewer,
            chapterFlags = row.chapter_flags,
            coverLastModified = row.cover_last_modified,
            url = row.url,
            ogTitle = row.title,
            ogArtist = row.artist,
            ogAuthor = row.author,
            ogThumbnailUrl = row.thumbnail_url,
            ogDescription = row.description,
            ogGenre = row.genre,
            ogStatus = row.status,
            updateStrategy = row.update_strategy,
            initialized = row.initialized,
            lastModifiedAt = row.last_modified_at,
            favoriteModifiedAt = row.favorite_modified_at,
            version = row.version,
            notes = row.notes,
            memo = row.memo,
        ),
        chapterCount = row.chapter_count,
    )
}
