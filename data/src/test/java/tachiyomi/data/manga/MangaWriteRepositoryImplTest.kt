package tachiyomi.data.manga

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.data.InjektHarness
import tachiyomi.domain.manga.model.MangaUpdate

internal class MangaWriteRepositoryImplTest {
    private val harness = InjektHarness()
    private val database = harness.database
    private val repository = MangaWriteRepositoryImpl(database)

    @BeforeEach
    fun installScope() {
        harness.install()
    }

    @AfterEach
    fun restoreScope() {
        harness.uninstall()
    }

    @Test
    fun resetViewerFlagsClearsViewer() = runTest {
        val id = database.insertManga(url = "/a")
        repository.update(MangaUpdate(id = id, viewerFlags = 5L)) shouldBe true

        repository.resetViewerFlags() shouldBe true

        database.mangasQueries.getMangaById(id).awaitAsOne().viewer shouldBe 0L
    }

    @Test
    fun setMangaCategoriesReplaces() = runTest {
        val id = database.insertManga(url = "/a")
        val category = database.categoriesQueries.insert(
            name = "Read", order = 1L, flags = 0L, version = 0L, uid = 1L, last_modified_at = 0L,
        ).awaitAsOne()

        repository.setMangaCategories(id, listOf(0L))
        repository.setMangaCategories(id, listOf(category))
        database.categoriesQueries.getCategoriesByMangaId(id).awaitAsList().map { it.id } shouldBe listOf(category)

        repository.setMangaCategories(id, emptyList())
        database.categoriesQueries.getCategoriesByMangaId(id).awaitAsList() shouldBe emptyList()
    }

    @Test
    fun updateAppliesGivenFields() = runTest {
        val id = database.insertManga(url = "/a", title = "Old")

        repository.update(MangaUpdate(id = id, title = "New", fetchInterval = 4, notes = "n")) shouldBe true

        val row = database.mangasQueries.getMangaById(id).awaitAsOne()
        row.title shouldBe "New"
        row.calculate_interval shouldBe 4L
        row.notes shouldBe "n"
    }

    @Test
    fun updateAllKeepsNullFields() = runTest {
        val id = database.insertManga(url = "/a", title = "Old")
        repository.updateAll(emptyList()) shouldBe true

        repository.updateAll(listOf(MangaUpdate(id = id, notes = "n"))) shouldBe true

        val row = database.mangasQueries.getMangaById(id).awaitAsOne()
        row.title shouldBe "Old"
        row.calculate_interval shouldBe 0L
    }

    @Test
    fun insertNetworkMangaInsertsNew() = runTest {
        val inserted = repository.insertNetworkManga(listOf(networkManga(url = "/new")))

        inserted.size shouldBe 1
        val manga = inserted.single()
        manga.id shouldNotBe -1L
        manga.ogTitle shouldBe "Title /new"
        manga.ogThumbnailUrl shouldBe "https://x//new.png"
        manga.ogAuthor shouldBe "author"
        manga.initialized shouldBe true
        database.mangasQueries.getMangaByUrlAndSource("/new", 1L).awaitAsOneOrNull()?._id shouldBe manga.id
    }

    @Test
    fun insertNetworkMangaEmptyList() = runTest {
        repository.insertNetworkManga(emptyList()) shouldBe emptyList()
    }

    @Test
    fun insertNetworkUpdatesExisting() = runTest {
        val id = database.insertManga(url = "/a", title = "Old")

        val manga = repository.insertNetworkManga(listOf(networkManga(url = "/a", title = "New"))).single()

        manga.id shouldBe id
        manga.ogTitle shouldBe "New"
        manga.ogThumbnailUrl shouldBe "https://x//a.png"
        manga.ogAuthor shouldBe "author"
        manga.ogGenre shouldBe listOf("a", "b")
        manga.initialized shouldBe true
    }

    @Test
    fun insertNetworkKeepsFavorite() = runTest {
        val id = database.insertManga(url = "/a", title = "Old", favorite = true)
        val fetched = networkManga(url = "/a", title = "New", favorite = true)

        val manga = repository.insertNetworkManga(listOf(fetched)).single()

        manga.id shouldBe id
        manga.ogTitle shouldBe "Old"
        manga.ogAuthor shouldBe null
        manga.favorite shouldBe true
    }

    @Test
    fun insertNetworkBlankTitle() = runTest {
        database.insertManga(url = "/a", title = "Old")
        val blank = networkManga(url = "/a", title = "", thumbnailUrl = null, initialized = false)

        val manga = repository.insertNetworkManga(listOf(blank)).single()

        manga.ogTitle shouldBe "Old"
        manga.ogThumbnailUrl shouldBe null
        manga.ogAuthor shouldBe null
        manga.initialized shouldBe false
    }

    @Test
    fun insertNetworkBlankCover() = runTest {
        database.insertManga(url = "/a", title = "Old")
        val blankCover = networkManga(url = "/a", title = "New", thumbnailUrl = " ")

        val manga = repository.insertNetworkManga(listOf(blankCover)).single()

        manga.ogTitle shouldBe "New"
        manga.ogThumbnailUrl shouldBe null
    }
}
