package tachiyomi.data.manga

import exh.source.EH_SOURCE_ID
import exh.source.EXH_SOURCE_ID
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.data.Database
import tachiyomi.data.InjektHarness

/** Inserts a `search_metadata` row for [mangaId]. */
internal suspend fun Database.insertMetadata(mangaId: Long, uploader: String? = "up") {
    search_metadataQueries.insert(
        manga_id = mangaId, uploader = uploader, extra = "{\"k\":1}", indexed_extra = "idx", extra_version = 2L,
    )
}

internal class MangaMetadataReadRepositoryImplTest {
    private val harness = InjektHarness()
    private val database = harness.database
    private val repository = MangaMetadataReadRepositoryImpl(database)

    @BeforeEach
    fun installScope() {
        harness.install()
    }

    @AfterEach
    fun restoreScope() {
        harness.uninstall()
    }

    @Test
    fun getMetadataById() = runTest {
        val id = database.insertManga(url = "/a")
        repository.getMetadataById(id) shouldBe null
        database.insertMetadata(mangaId = id)

        val metadata = requireNotNull(repository.getMetadataById(id))

        metadata.mangaId shouldBe id
        metadata.uploader shouldBe "up"
        metadata.extra shouldBe "{\"k\":1}"
        metadata.indexedExtra shouldBe "idx"
        metadata.extraVersion shouldBe 2
    }

    @Test
    fun subscribeMetadataById() = runTest {
        val id = database.insertManga(url = "/a")
        repository.subscribeMetadataById(id).first() shouldBe null
        database.insertMetadata(mangaId = id, uploader = null)

        repository.subscribeMetadataById(id).first()?.uploader shouldBe null
    }

    @Test
    fun getTagsById() = runTest {
        val id = database.insertManga(url = "/a")
        val other = database.insertManga(url = "/b")
        database.search_tagsQueries.insert(manga_id = id, namespace = "ns", name = "one", type = 1L)
        database.search_tagsQueries.insert(manga_id = id, namespace = null, name = "two", type = 2L)
        database.search_tagsQueries.insert(manga_id = other, namespace = "ns", name = "three", type = 3L)

        val tags = repository.getTagsById(id)

        tags.map { it.name } shouldBe listOf("one", "two")
        tags.first().namespace shouldBe "ns"
        tags.first().type shouldBe 1
        repository.subscribeTagsById(other).first().map { it.name } shouldBe listOf("three")
    }

    @Test
    fun getTitlesById() = runTest {
        val id = database.insertManga(url = "/a")
        val other = database.insertManga(url = "/b")
        database.search_titlesQueries.insert(manga_id = id, title = "one", type = 1L)
        database.search_titlesQueries.insert(manga_id = other, title = "two", type = 2L)

        val titles = repository.getTitlesById(id)

        titles.map { it.title } shouldBe listOf("one")
        titles.single().type shouldBe 1
        repository.subscribeTitlesById(other).first().map { it.title } shouldBe listOf("two")
    }

    @Test
    fun getExhFavoritesWithMetadata() = runTest {
        val eh = database.insertManga(url = "/a", source = EH_SOURCE_ID, favorite = true)
        val exh = database.insertManga(url = "/b", source = EXH_SOURCE_ID, favorite = true)
        val plain = database.insertManga(url = "/c", source = 1L, favorite = true)
        val notFavorite = database.insertManga(url = "/d", source = EH_SOURCE_ID)
        database.insertManga(url = "/e", source = EH_SOURCE_ID, favorite = true)
        listOf(eh, exh, plain, notFavorite).forEach { database.insertMetadata(mangaId = it) }

        repository.getExhFavoritesWithMetadata().map { it.id }.sorted() shouldBe listOf(eh, exh)
        repository.getFavoriteIdsWithMetadata().sorted() shouldBe listOf(eh, exh, plain)
        repository.getSearchMetadata().map { it.mangaId }.sorted() shouldBe listOf(eh, exh, plain, notFavorite)
    }

    @Test
    fun emptyTablesGiveEmptyLists() = runTest {
        repository.getSearchMetadata() shouldBe emptyList()
        repository.getFavoriteIdsWithMetadata() shouldBe emptyList()
        repository.getExhFavoritesWithMetadata() shouldBe emptyList()
        repository.getTagsById(1L) shouldBe emptyList()
        repository.getTitlesById(1L) shouldBe emptyList()
    }
}
