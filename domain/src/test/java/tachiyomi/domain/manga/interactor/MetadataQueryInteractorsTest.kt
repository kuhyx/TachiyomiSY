package tachiyomi.domain.manga.interactor

import exh.metadata.sql.models.SearchMetadata
import exh.metadata.sql.models.SearchTag
import exh.metadata.sql.models.SearchTitle
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.manga.model.MangaFixtures
import tachiyomi.domain.manga.repository.MangaMetadataRepository

internal class MetadataQueryInteractorsTest {

    private val metadata = SearchMetadata(
        mangaId = 4L,
        uploader = "up",
        extra = "{}",
        indexedExtra = null,
        extraVersion = 0,
    )
    private val tag = SearchTag(id = 1L, mangaId = 4L, namespace = "artist", name = "ann", type = 0)
    private val title = SearchTitle(id = 1L, mangaId = 4L, title = "main", type = 0)
    private val repository = mockk<MangaMetadataRepository>()

    @Test
    fun exhFavoritesDelegates() = runTest {
        val manga = MangaFixtures.manga(id = 4L)
        coEvery { repository.getExhFavoritesWithMetadata() } returns listOf(manga)

        GetExhFavoriteMangaWithMetadata(repository).await() shouldContainExactly listOf(manga)
    }

    @Test
    fun favoriteIdsDelegates() = runTest {
        coEvery { repository.getFavoriteIdsWithMetadata() } returns listOf(4L, 5L)

        GetIdsOfFavoriteMangaWithMetadata(repository).await() shouldContainExactly listOf(4L, 5L)
    }

    @Test
    fun searchMetadataByIdDelegates() = runTest {
        coEvery { repository.getMetadataById(4L) } returns metadata
        coEvery { repository.getMetadataById(5L) } returns null

        GetSearchMetadata(repository).await(4L) shouldBe metadata
        GetSearchMetadata(repository).await(5L) shouldBe null
    }

    @Test
    fun allSearchMetadataDelegates() = runTest {
        coEvery { repository.getSearchMetadata() } returns listOf(metadata)

        GetSearchMetadata(repository).await() shouldContainExactly listOf(metadata)
    }

    @Test
    fun searchTagsDelegates() = runTest {
        coEvery { repository.getTagsById(4L) } returns listOf(tag)

        GetSearchTags(repository).await(4L) shouldContainExactly listOf(tag)
    }

    @Test
    fun searchTitlesDelegates() = runTest {
        coEvery { repository.getTitlesById(4L) } returns listOf(title)

        GetSearchTitles(repository).await(4L) shouldContainExactly listOf(title)
    }
}
