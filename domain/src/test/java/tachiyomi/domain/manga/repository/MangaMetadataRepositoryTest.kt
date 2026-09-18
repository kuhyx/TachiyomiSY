package tachiyomi.domain.manga.repository

import exh.metadata.metadata.RaisedSearchMetadata
import exh.metadata.metadata.RankedSearchMetadata
import exh.metadata.metadata.base.FlatMetadata
import exh.metadata.sql.models.SearchMetadata
import exh.metadata.sql.models.SearchTag
import exh.metadata.sql.models.SearchTitle
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.manga.model.Manga

/** Only records what the default [MangaMetadataRepository.insertMetadata] hands to [insertFlatMetadata]. */
private class RecordingMetadataRepository : MangaMetadataRepository {
    val inserted = mutableListOf<FlatMetadata>()

    override suspend fun insertFlatMetadata(flatMetadata: FlatMetadata) {
        inserted += flatMetadata
    }

    override suspend fun getMetadataById(id: Long): SearchMetadata? = inserted.firstOrNull()?.metadata

    override fun subscribeMetadataById(id: Long): Flow<SearchMetadata?> = flowOf(inserted.firstOrNull()?.metadata)

    override suspend fun getTagsById(id: Long): List<SearchTag> = inserted.flatMap { it.tags }

    override fun subscribeTagsById(id: Long): Flow<List<SearchTag>> = flowOf(inserted.flatMap { it.tags })

    override suspend fun getTitlesById(id: Long): List<SearchTitle> = inserted.flatMap { it.titles }

    override fun subscribeTitlesById(id: Long): Flow<List<SearchTitle>> = flowOf(inserted.flatMap { it.titles })

    override suspend fun getExhFavoritesWithMetadata(): List<Manga> =
        inserted.map { Manga.create().copy(id = it.metadata.mangaId) }

    override suspend fun getFavoriteIdsWithMetadata(): List<Long> = inserted.map { it.metadata.mangaId }

    override suspend fun getSearchMetadata(): List<SearchMetadata> = inserted.map { it.metadata }
}

internal class MangaMetadataRepositoryTest {

    private val repository = RecordingMetadataRepository()

    @Test
    fun insertMetadataFlattensFirst() = runTest {
        val raised: RaisedSearchMetadata = RankedSearchMetadata().apply {
            mangaId = 4L
            uploader = "up"
        }

        repository.insertMetadata(raised)

        repository.inserted shouldContainExactly listOf(raised.flatten())
        repository.inserted.single().metadata.mangaId shouldBe 4L
        repository.inserted.single().metadata.uploader shouldBe "up"
    }

    @Test
    fun insertMetadataNeedsMangaId() = runTest {
        shouldThrow<IllegalArgumentException> { repository.insertMetadata(RankedSearchMetadata()) }

        repository.inserted shouldBe emptyList()
    }
}
