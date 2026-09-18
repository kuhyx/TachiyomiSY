package tachiyomi.data.manga

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.data.GetDuplicateLibraryManga
import tachiyomi.data.GetMangasWithFavoriteTimestamp
import tachiyomi.data.Mangas
import tachiyomi.domain.manga.model.Manga
import tachiyomi.view.LibraryView

internal class MangaMapperTest {
    private val dated = mangasRow(id = 5L, lastUpdate = 100L, nextUpdate = 200L)
    private val undated = mangasRow(id = 6L)

    @Test
    fun mapMangaCopiesEveryColumn() {
        val manga = MangaMapper.mapManga(dated)
        manga.assertMatches(dated)
        manga.lastUpdate shouldBe 100L
        manga.nextUpdate shouldBe 200L
    }

    @Test
    fun mapMangaDefaultsNullDates() {
        val manga = MangaMapper.mapManga(undated)
        manga.lastUpdate shouldBe 0L
        manga.nextUpdate shouldBe 0L
    }

    @Test
    fun mapTimestampRow() {
        val manga = MangaMapper.mapManga(dated.toTimestampRow())
        manga.assertMatches(dated)
        manga.lastUpdate shouldBe 100L
        manga.nextUpdate shouldBe 200L
    }

    @Test
    fun mapTimestampRowNullDates() {
        val manga = MangaMapper.mapManga(undated.toTimestampRow())
        manga.lastUpdate shouldBe 0L
        manga.nextUpdate shouldBe 0L
    }

    @Test
    fun mapLibraryMangaCopiesCounts() {
        val library = MangaMapper.mapLibraryManga(dated.toLibraryView())
        library.manga.assertMatches(dated)
        library.manga.lastUpdate shouldBe 100L
        library.manga.nextUpdate shouldBe 200L
        library.categories shouldBe listOf(1L, 2L)
        library.totalChapters shouldBe 10L
        library.readCount shouldBe 4L
        library.bookmarkCount shouldBe 2L
        library.latestUpload shouldBe 30L
        library.chapterFetchedAt shouldBe 40L
        library.lastRead shouldBe 50L
    }

    @Test
    fun mapLibraryMangaNullDates() {
        val library = MangaMapper.mapLibraryManga(undated.toLibraryView())
        library.manga.lastUpdate shouldBe 0L
        library.manga.nextUpdate shouldBe 0L
    }

    @Test
    fun mapWithChapterCount() {
        val result = MangaMapper.mapMangaWithChapterCount(dated.toDuplicateRow())
        result.manga.assertMatches(dated)
        result.manga.lastUpdate shouldBe 100L
        result.manga.nextUpdate shouldBe 200L
        result.chapterCount shouldBe 7L
    }

    @Test
    fun mapWithChapterCountNullDates() {
        val result = MangaMapper.mapMangaWithChapterCount(undated.toDuplicateRow())
        result.manga.lastUpdate shouldBe 0L
        result.manga.nextUpdate shouldBe 0L
    }

    private fun Manga.assertMatches(row: Mangas) {
        id shouldBe row._id
        source shouldBe row.source
        favorite shouldBe row.favorite
        fetchInterval shouldBe row.calculate_interval.toInt()
        dateAdded shouldBe row.date_added
        viewerFlags shouldBe row.viewer
        chapterFlags shouldBe row.chapter_flags
        coverLastModified shouldBe row.cover_last_modified
        url shouldBe row.url
        ogTitle shouldBe row.title
        ogArtist shouldBe row.artist
        ogAuthor shouldBe row.author
        ogThumbnailUrl shouldBe row.thumbnail_url
        ogDescription shouldBe row.description
        ogGenre shouldBe row.genre
        ogStatus shouldBe row.status
        updateStrategy shouldBe row.update_strategy
        initialized shouldBe row.initialized
        lastModifiedAt shouldBe row.last_modified_at
        favoriteModifiedAt shouldBe row.favorite_modified_at
        version shouldBe row.version
        notes shouldBe row.notes
        memo shouldBe row.memo
    }

    private fun Mangas.toTimestampRow(): GetMangasWithFavoriteTimestamp = GetMangasWithFavoriteTimestamp(
        _id = _id, source = source, url = url, artist = artist, author = author, description = description,
        genre = genre, title = title, status = status, thumbnail_url = thumbnail_url, favorite = favorite,
        last_update = last_update, next_update = next_update, initialized = initialized, viewer = viewer,
        chapter_flags = chapter_flags, cover_last_modified = cover_last_modified, date_added = date_added,
        filtered_scanlators = filtered_scanlators, update_strategy = update_strategy,
        calculate_interval = calculate_interval, last_modified_at = last_modified_at,
        favorite_modified_at = requireNotNull(favorite_modified_at), version = version, is_syncing = is_syncing,
        notes = notes, memo = memo,
    )

    private fun Mangas.toLibraryView(): LibraryView = LibraryView(
        _id = _id, source = source, url = url, artist = artist, author = author, description = description,
        genre = genre, title = title, status = status, thumbnail_url = thumbnail_url, favorite = favorite,
        last_update = last_update, next_update = next_update, initialized = initialized, viewer = viewer,
        chapter_flags = chapter_flags, cover_last_modified = cover_last_modified, date_added = date_added,
        filtered_scanlators = filtered_scanlators, update_strategy = update_strategy,
        calculate_interval = calculate_interval, last_modified_at = last_modified_at,
        favorite_modified_at = favorite_modified_at, version = version, is_syncing = is_syncing, notes = notes,
        memo = memo, totalCount = 10L, readCount = 4.0, latestUpload = 30L, chapterFetchedAt = 40L, lastRead = 50L,
        bookmarkCount = 2.0, categories = "1,2",
    )

    private fun Mangas.toDuplicateRow(): GetDuplicateLibraryManga = GetDuplicateLibraryManga(
        _id = _id, source = source, url = url, artist = artist, author = author, description = description,
        genre = genre, title = title, status = status, thumbnail_url = thumbnail_url, favorite = favorite,
        last_update = last_update, next_update = next_update, initialized = initialized, viewer = viewer,
        chapter_flags = chapter_flags, cover_last_modified = cover_last_modified, date_added = date_added,
        filtered_scanlators = filtered_scanlators, update_strategy = update_strategy,
        calculate_interval = calculate_interval, last_modified_at = last_modified_at,
        favorite_modified_at = favorite_modified_at, version = version, is_syncing = is_syncing, notes = notes,
        memo = memo, chapter_count = 7L,
    )
}
