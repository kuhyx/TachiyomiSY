package tachiyomi.data.manga

import exh.source.MERGED_SOURCE_ID
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.data.InjektHarness
import tachiyomi.data.seedChapter

internal class MangaLibraryRepositoryImplTest {
    private val harness = InjektHarness()
    private val database = harness.database
    private val repository = MangaLibraryRepositoryImpl(database)

    @BeforeEach
    fun installScope() {
        harness.install()
    }

    @AfterEach
    fun restoreScope() {
        harness.uninstall()
    }

    @Test
    fun getFavoritesOnlyFavorites() = runTest {
        val favorite = database.insertManga(url = "/a", favorite = true)
        database.insertManga(url = "/b")

        repository.getFavorites().map { it.id } shouldBe listOf(favorite)
    }

    @Test
    fun getReadMangaNotInLibrary() = runTest {
        val read = database.insertManga(url = "/a")
        database.seedChapter(mangaId = read, read = true)
        val partlyRead = database.insertManga(url = "/b")
        database.seedChapter(mangaId = partlyRead, lastPageRead = 3L)
        val unread = database.insertManga(url = "/c")
        database.seedChapter(mangaId = unread)
        val favorite = database.insertManga(url = "/d", favorite = true)
        database.seedChapter(mangaId = favorite, read = true)

        repository.getReadMangaNotInLibrary().map { it.id } shouldBe listOf(read, partlyRead)
    }

    @Test
    fun getLibraryMangaAggregates() = runTest {
        val favorite = database.insertManga(url = "/a", favorite = true)
        database.seedChapter(mangaId = favorite, read = true, bookmark = true)
        database.seedChapter(mangaId = favorite)
        database.mangas_categoriesQueries.insert(mangaId = favorite, categoryId = 0L)
        database.insertManga(url = "/b")

        val library = repository.getLibraryManga()

        val entry = library.single()
        entry.id shouldBe favorite
        entry.totalChapters shouldBe 2L
        entry.readCount shouldBe 1L
        entry.bookmarkCount shouldBe 1L
        entry.latestUpload shouldBe 5L
        entry.chapterFetchedAt shouldBe 10L
        entry.categories shouldBe listOf(0L)
    }

    @Test
    fun getLibraryMangaMergedSource() = runTest {
        val merged = database.insertManga(url = "/merged", source = MERGED_SOURCE_ID, favorite = true)
        val part = database.insertManga(url = "/part")
        database.seedChapter(mangaId = part, read = true)
        database.mergedQueries.insert(
            infoManga = true, getChapterUpdates = true, chapterSortMode = 0L, chapterPriority = 0L,
            downloadChapters = true, mergeId = merged, mergeUrl = "/merged", mangaId = part, mangaUrl = "/part",
            mangaSource = 1L,
        )

        val entry = repository.getLibraryManga().single()

        entry.id shouldBe merged
        entry.totalChapters shouldBe 1L
        entry.readCount shouldBe 1L
        entry.categories shouldBe listOf(0L)
    }

    @Test
    fun getLibraryMangaAsFlow() = runTest {
        val favorite = database.insertManga(url = "/a", favorite = true)

        repository.getLibraryMangaAsFlow().first().map { it.id } shouldBe listOf(favorite)
    }

    @Test
    fun getReadMangaNotInLibraryView() = runTest {
        val read = database.insertManga(url = "/a")
        database.seedChapter(mangaId = read, read = true)
        val unread = database.insertManga(url = "/b")
        database.seedChapter(mangaId = unread)
        val favorite = database.insertManga(url = "/c", favorite = true)
        database.seedChapter(mangaId = favorite, read = true)

        val entries = repository.getReadMangaNotInLibraryView()

        entries.map { it.id } shouldBe listOf(read)
        entries.single().readCount shouldBe 1L
    }
}
