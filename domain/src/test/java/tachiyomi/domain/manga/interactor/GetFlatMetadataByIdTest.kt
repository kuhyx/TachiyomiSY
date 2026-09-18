package tachiyomi.domain.manga.interactor

import exh.metadata.metadata.base.FlatMetadata
import exh.metadata.sql.models.SearchMetadata
import exh.metadata.sql.models.SearchTag
import exh.metadata.sql.models.SearchTitle
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.manga.repository.MangaMetadataRepository

internal class GetFlatMetadataByIdTest {

    private val metadata = SearchMetadata(
        mangaId = 4L,
        uploader = null,
        extra = "{}",
        indexedExtra = "x",
        extraVersion = 1,
    )
    private val tags = listOf(SearchTag(id = null, mangaId = 4L, namespace = null, name = "loose", type = 2))
    private val titles = listOf(SearchTitle(id = null, mangaId = 4L, title = "main", type = 0))
    private val flat = FlatMetadata(metadata = metadata, tags = tags, titles = titles)
    private val repository = mockk<MangaMetadataRepository>()
    private val getFlatMetadataById = GetFlatMetadataById(repository)

    @Test
    fun awaitAssemblesFlatMetadata() = runTest {
        coEvery { repository.getMetadataById(4L) } returns metadata
        coEvery { repository.getTagsById(4L) } returns tags
        coEvery { repository.getTitlesById(4L) } returns titles

        getFlatMetadataById.await(4L) shouldBe flat
    }

    @Test
    fun awaitIsNullWithoutMetadata() = runTest {
        coEvery { repository.getMetadataById(4L) } returns null

        getFlatMetadataById.await(4L) shouldBe null
    }

    @Test
    fun awaitSwallowsFailure() = runTest {
        coEvery { repository.getMetadataById(4L) } throws IllegalStateException("store failed")

        getFlatMetadataById.await(4L) shouldBe null
    }

    @Test
    fun subscribeCombinesFlows() = runTest {
        every { repository.subscribeMetadataById(4L) } returns flowOf(metadata)
        every { repository.subscribeTagsById(4L) } returns flowOf(tags)
        every { repository.subscribeTitlesById(4L) } returns flowOf(titles)

        getFlatMetadataById.subscribe(4L).first() shouldBe flat
    }

    @Test
    fun subscribeNullWithoutMeta() = runTest {
        every { repository.subscribeMetadataById(4L) } returns flowOf(null)
        every { repository.subscribeTagsById(4L) } returns flowOf(tags)
        every { repository.subscribeTitlesById(4L) } returns flowOf(titles)

        getFlatMetadataById.subscribe(4L).first() shouldBe null
    }
}
