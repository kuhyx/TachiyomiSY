package tachiyomi.data.source

import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import exh.metadata.metadata.EHentaiSearchMetadata
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import java.io.IOException

/** A paging source that passes its dependency explicitly and may have no source at all. */
internal class ExplicitPagingSource(
    source: Source?,
    networkToLocalManga: NetworkToLocalManga,
    private val page: MangasPage,
) : BaseSourcePagingSource(source, networkToLocalManga) {
    override suspend fun requestNextPage(currentPage: Int): MangasPage = page
}

internal class SourcePagingSourceTest {
    private val env = InjektEnv()
    private val source = mockSource(sourceId = 1L)

    @BeforeEach
    fun setUp() {
        env.install()
    }

    @AfterEach
    fun tearDown() {
        env.restore()
    }

    @Test
    fun refreshLoadsFirstPage() = runTest {
        coEvery { source.getPopularManga(1) } returns pageOf("a", "b", hasNextPage = true)

        val page = SourcePopularPagingSource(source).load(refresh()).page()

        page.urlsAndIds() shouldBe listOf("a" to 100L, "b" to 101L)
        page.data.map { it.first.source } shouldBe listOf(1L, 1L)
        page.data.map { it.second } shouldBe listOf(null, null)
        page.prevKey shouldBe null
        page.nextKey shouldBe 2L
    }

    @Test
    fun appendUsesTheKey() = runTest {
        coEvery { source.getPopularManga(3) } returns pageOf("c")

        val page = SourcePopularPagingSource(source).load(append(3L)).page()

        page.urlsAndIds() shouldBe listOf("c" to 100L)
        page.nextKey shouldBe null
    }

    @Test
    fun emptyPageIsNoResults() = runTest {
        coEvery { source.getPopularManga(1) } returns pageOf()

        SourcePopularPagingSource(source).load(refresh()).error().shouldBeInstanceOf<NoResultsException>()
    }

    @Test
    fun sourceFailureIsAnError() = runTest {
        coEvery { source.getPopularManga(1) } throws IOException("down")

        val error = SourcePopularPagingSource(source).load(refresh()).error()

        error.shouldBeInstanceOf<IOException>()
        error.message shouldBe "down"
    }

    @Test
    fun seenUrlsAreSkipped() = runTest {
        coEvery { source.getPopularManga(1) } returns pageOf("a", "a", "b", hasNextPage = true)
        coEvery { source.getPopularManga(2) } returns pageOf("b", "c")
        val pagingSource = SourcePopularPagingSource(source)

        pagingSource.load(refresh()).page().urlsAndIds() shouldBe listOf("a" to 100L, "b" to 101L)
        pagingSource.load(append(2L)).page().urlsAndIds() shouldBe listOf("c" to 100L)
    }

    @Test
    fun metadataIsPairedByPosition() = runTest {
        val metadata = EHentaiSearchMetadata()
        coEvery { source.getPopularManga(1) } returns metadataPageOf(listOf("a", "b"), listOf(metadata))

        val page = SourcePopularPagingSource(source).load(refresh()).page()

        page.data.map { it.second } shouldBe listOf(metadata, null)
    }

    @Test
    fun searchRequestsTheQuery() = runTest {
        val filters = FilterList()
        coEvery { source.getSearchManga(1, "q", filters) } returns pageOf("a")

        SourceSearchPagingSource(source, "q", filters).load(refresh()).page().urlsAndIds() shouldBe
            listOf("a" to 100L)

        coVerify(exactly = 1) { source.getSearchManga(1, "q", filters) }
    }

    @Test
    fun latestRequestsLatestUpdates() = runTest {
        coEvery { source.getLatestUpdates(1) } returns pageOf("a")

        SourceLatestPagingSource(source).load(refresh()).page().urlsAndIds() shouldBe listOf("a" to 100L)
    }

    @Test
    fun explicitDependencyIsUsed() = runTest {
        val explicit = mockk<NetworkToLocalManga>()
        coEvery { explicit.invoke(any<List<Manga>>()) } answers {
            firstArg<List<Manga>>().map { it.copy(id = 7L) }
        }
        val pagingSource = ExplicitPagingSource(source, explicit, pageOf("a"))

        pagingSource.load(refresh()).page().urlsAndIds() shouldBe listOf("a" to 7L)
    }

    @Test
    fun missingSourceIsAnError() = runTest {
        val pagingSource = ExplicitPagingSource(null, localizingNtl(), pageOf("a"))

        pagingSource.load(refresh()).error().shouldBeInstanceOf<NullPointerException>()
    }
}
