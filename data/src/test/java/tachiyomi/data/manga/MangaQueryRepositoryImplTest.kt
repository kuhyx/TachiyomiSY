package tachiyomi.data.manga

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.data.InjektHarness
import tachiyomi.data.seedChapter

internal class MangaQueryRepositoryImplTest {
    private val harness = InjektHarness()
    private val database = harness.database
    private val repository = MangaQueryRepositoryImpl(database)

    @BeforeEach
    fun installScope() {
        harness.install()
    }

    @AfterEach
    fun restoreScope() {
        harness.uninstall()
    }

    @Test
    fun getMangaByIdMapsRow() = runTest {
        val id = database.insertManga(url = "/a", title = "A")

        val manga = repository.getMangaById(id)

        manga.id shouldBe id
        manga.url shouldBe "/a"
        manga.ogTitle shouldBe "A"
        repository.getMangaByIdAsFlow(id).first().id shouldBe id
    }

    @Test
    fun getMangaByUrlAndSourceId() = runTest {
        val id = database.insertManga(url = "/a", source = 2L)

        repository.getMangaByUrlAndSourceId("/a", 2L)?.id shouldBe id
        repository.getMangaByUrlAndSourceId("/a", 3L) shouldBe null
        repository.getMangaByUrlAndSourceIdAsFlow("/a", 2L).first()?.id shouldBe id
        repository.getMangaByUrlAndSourceIdAsFlow("/missing", 2L).first() shouldBe null
    }

    @Test
    fun getFavoritesBySourceId() = runTest {
        val favorite = database.insertManga(url = "/a", source = 2L, favorite = true)
        database.insertManga(url = "/b", source = 2L)
        database.insertManga(url = "/c", source = 3L, favorite = true)

        repository.getFavoritesBySourceId(2L).first().map { it.id } shouldBe listOf(favorite)
    }

    @Test
    fun getDuplicateLibraryManga() = runTest {
        val original = database.insertManga(url = "/a", title = "One Piece", favorite = true)
        val duplicate = database.insertManga(url = "/b", title = "one piece (colored)", favorite = true)
        database.seedChapter(mangaId = duplicate)
        database.seedChapter(mangaId = duplicate)
        database.insertManga(url = "/c", title = "One Piece", favorite = false)

        val result = repository.getDuplicateLibraryManga(original, "piece")

        result.map { it.manga.id } shouldBe listOf(duplicate)
        result.single().chapterCount shouldBe 2L
    }

    @Test
    fun getUpcomingManga() = runTest {
        val soon = database.insertManga(url = "/a", favorite = true, nextUpdate = Long.MAX_VALUE, status = 1L)
        database.insertManga(url = "/b", favorite = true, nextUpdate = Long.MAX_VALUE, status = 2L)
        database.insertManga(url = "/c", favorite = true, nextUpdate = 0L, status = 1L)
        database.insertManga(url = "/d", favorite = false, nextUpdate = Long.MAX_VALUE, status = 1L)

        repository.getUpcomingManga(setOf(1L)).first().map { it.id } shouldBe listOf(soon)
    }

    @Test
    fun getMangaBySourceId() = runTest {
        val first = database.insertManga(url = "/a", source = 2L)
        val second = database.insertManga(url = "/b", source = 2L)
        database.insertManga(url = "/c", source = 3L)

        repository.getMangaBySourceId(2L).map { it.id } shouldBe listOf(first, second)
    }

    @Test
    fun getAllReturnsEveryRow() = runTest {
        repository.getAll() shouldBe emptyList()
        val first = database.insertManga(url = "/a")
        val second = database.insertManga(url = "/b", favorite = true)

        repository.getAll().map { it.id } shouldBe listOf(first, second)
    }
}
