package tachiyomi.data.manga

import exh.metadata.metadata.base.FlatMetadata
import exh.metadata.sql.models.SearchMetadata
import exh.metadata.sql.models.SearchTag
import exh.metadata.sql.models.SearchTitle
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.data.inMemoryDatabase

internal class MangaMetadataRepositoryImplTest {
    private val database = inMemoryDatabase()
    private val repository = MangaMetadataRepositoryImpl(database)

    private fun metadata(mangaId: Long, extra: String = "{}"): SearchMetadata = SearchMetadata(
        mangaId = mangaId, uploader = "up", extra = extra, indexedExtra = null, extraVersion = 1,
    )

    @Test
    fun insertFlatMetadataStoresAll() = runTest {
        val id = database.insertManga(url = "/a")
        val flat = FlatMetadata(
            metadata = metadata(mangaId = id),
            tags = listOf(SearchTag(id = null, mangaId = id, namespace = "ns", name = "tag", type = 1)),
            titles = listOf(SearchTitle(id = null, mangaId = id, title = "title", type = 2)),
        )

        repository.insertFlatMetadata(flat)

        repository.getMetadataById(id)?.uploader shouldBe "up"
        repository.getTagsById(id).map { it.name } shouldBe listOf("tag")
        repository.getTitlesById(id).map { it.title } shouldBe listOf("title")
    }

    @Test
    fun insertFlatMetadataReplaces() = runTest {
        val id = database.insertManga(url = "/a")
        val first = FlatMetadata(
            metadata = metadata(mangaId = id),
            tags = listOf(SearchTag(id = null, mangaId = id, namespace = null, name = "old", type = 1)),
            titles = listOf(SearchTitle(id = null, mangaId = id, title = "old", type = 1)),
        )
        repository.insertFlatMetadata(first)

        val replacement = metadata(mangaId = id, extra = "{\"v\":2}")
        repository.insertFlatMetadata(FlatMetadata(metadata = replacement, tags = emptyList(), titles = emptyList()))

        repository.getMetadataById(id)?.extra shouldBe "{\"v\":2}"
        repository.getSearchMetadata().size shouldBe 1
        repository.getTagsById(id) shouldBe emptyList()
        repository.getTitlesById(id) shouldBe emptyList()
    }

    @Test
    fun insertMetadataRejectsUnsaved() = runTest {
        val flat = FlatMetadata(metadata = metadata(mangaId = -1L), tags = emptyList(), titles = emptyList())

        shouldThrow<IllegalArgumentException> { repository.insertFlatMetadata(flat) }

        repository.getSearchMetadata() shouldBe emptyList()
    }
}
