package tachiyomi.data.source

import eu.kanade.tachiyomi.source.model.FilterList
import exh.metadata.metadata.EHentaiSearchMetadata
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class EHentaiPagingSourceTest {
    private val env = InjektEnv()
    private val source = mockSource(sourceId = 6L)
    private val metadata = EHentaiSearchMetadata()

    @BeforeEach
    fun setUp() {
        env.install()
    }

    @AfterEach
    fun tearDown() {
        env.restore()
    }

    @Test
    fun popularPairsMetadataAndKey() = runTest {
        coEvery { source.getPopularManga(1) } returns metadataPageOf(
            urls = listOf("a", "b"),
            metadata = listOf(metadata),
            hasNextPage = true,
            nextKey = 42L,
        )

        val page = EHentaiPopularPagingSource(source).load(refresh()).page()

        page.urlsAndIds() shouldBe listOf("a" to 100L, "b" to 101L)
        page.data.map { it.first.source } shouldBe listOf(6L, 6L)
        page.data.map { it.second } shouldBe listOf(metadata, null)
        page.prevKey shouldBe null
        page.nextKey shouldBe 42L
    }

    @Test
    fun appendKeepsDuplicates() = runTest {
        coEvery { source.getPopularManga(5) } returns metadataPageOf(listOf("a", "a"), emptyList())

        val page = EHentaiPopularPagingSource(source).load(append(5L)).page()

        page.urlsAndIds() shouldBe listOf("a" to 100L, "a" to 101L)
        page.nextKey shouldBe null
    }

    @Test
    fun searchRequestsTheQuery() = runTest {
        val filters = FilterList()
        coEvery { source.getSearchManga(1, "q", filters) } returns metadataPageOf(listOf("a"), emptyList())
        val pagingSource = EHentaiSearchPagingSource(source, "q", filters)

        pagingSource.query shouldBe "q"
        pagingSource.filters shouldBe filters
        pagingSource.load(refresh()).page().urlsAndIds() shouldBe listOf("a" to 100L)
        coVerify(exactly = 1) { source.getSearchManga(1, "q", filters) }
    }

    @Test
    fun latestRequestsLatestUpdates() = runTest {
        coEvery { source.getLatestUpdates(2) } returns metadataPageOf(listOf("z"), emptyList())

        EHentaiLatestPagingSource(source).load(append(2L)).page().urlsAndIds() shouldBe listOf("z" to 100L)
    }

    @Test
    fun plainPageIsAnError() = runTest {
        coEvery { source.getPopularManga(1) } returns pageOf("a")

        EHentaiPopularPagingSource(source).load(refresh()).error().shouldBeInstanceOf<ClassCastException>()
    }

    @Test
    fun emptyPageIsNoResults() = runTest {
        coEvery { source.getPopularManga(1) } returns metadataPageOf(emptyList(), emptyList())

        EHentaiPopularPagingSource(source).load(refresh()).error().shouldBeInstanceOf<NoResultsException>()
    }
}
