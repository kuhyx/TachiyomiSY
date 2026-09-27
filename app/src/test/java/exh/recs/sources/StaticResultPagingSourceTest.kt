package exh.recs.sources

import androidx.paging.PagingSource
import eu.kanade.tachiyomi.source.model.MetadataMangasPage
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.InjektStub
import exh.metadata.metadata.RankedSearchMetadata
import exh.recs.batch.RankedSearchResults
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.sy.SYMR

internal fun rankedResults(count: Int, associatedSourceId: Long? = 7L): RankedSearchResults = RankedSearchResults(
    recSourceName = "Static",
    recSourceCategoryResId = SYMR.strings.similar_titles.resourceId,
    recAssociatedSourceId = associatedSourceId,
    results = (1..count).associate { index -> SManga(url = "/m$index", title = "Manga $index") to index },
)

internal class StaticResultPagingSourceTest {
    private val stub = InjektStub()

    @BeforeEach
    fun setUp() {
        stub.install()
        val networkToLocal = mockk<NetworkToLocalManga>()
        coEvery { networkToLocal(any<Manga>()) } answers { firstArg() }
        coEvery { networkToLocal(any<List<Manga>>()) } answers { firstArg<List<Manga>>().map { it.copy(id = 42L) } }
        stub.serve(networkToLocal)
    }

    @AfterEach
    fun tearDown() = stub.uninstall()

    @Test
    fun identityFromResults() {
        val source = StaticResultPagingSource(rankedResults(1))
        source.name shouldBe "Static"
        source.category.resourceId shouldBe SYMR.strings.similar_titles.resourceId
        source.associatedSourceId shouldBe 7L
        StaticResultPagingSource(rankedResults(1, associatedSourceId = null)).associatedSourceId shouldBe null
        StaticResultPagingSource.PAGE_SIZE shouldBe 25
    }

    @Test
    fun firstPageOfAShortList() {
        val source = StaticResultPagingSource(rankedResults(2))
        val page = runBlocking { source.requestNextPage(1) } as MetadataMangasPage
        page.mangas.map { it.title } shouldContainExactly listOf("Manga 1", "Manga 2")
        page.hasNextPage shouldBe false
        page.mangasMetadata.map { (it as RankedSearchMetadata).rank } shouldContainExactly listOf(1, 2)
    }

    @Test
    fun virtualPagingChunks() {
        val source = StaticResultPagingSource(rankedResults(30))
        val first = runBlocking { source.requestNextPage(1) }
        first.mangas.size shouldBe 25
        first.hasNextPage shouldBe true
        val second = runBlocking { source.requestNextPage(2) }
        second.mangas.size shouldBe 5
        second.hasNextPage shouldBe false
        runBlocking { source.requestNextPage(3) }.mangas.isEmpty() shouldBe true
        runBlocking { source.requestNextPage(0) }.mangas.isEmpty() shouldBe true
    }

    @Test
    fun loadSavesUnderAssociated() {
        val page = StaticResultPagingSource(rankedResults(2)).loadFirst()
        page.data.map { it.first.source to it.first.id } shouldContainExactly listOf(7L to 42L, 7L to 42L)
        page.data.map { (it.second as RankedSearchMetadata).rank } shouldContainExactly listOf(1, 2)
    }

    @Test
    fun loadLeavesUnassociated() {
        val page = StaticResultPagingSource(rankedResults(1, associatedSourceId = null)).loadFirst()
        page.data.single().first.source shouldBe -1L
        page.data.single().first.id shouldBe -1L
    }

    @Test
    fun resultsDataClass() {
        val results = rankedResults(1)
        results.copy(recSourceName = "Other").recSourceName shouldBe "Other"
        results.hashCode() shouldBe results.copy().hashCode()
        results.toString().contains("Static") shouldBe true
    }
}

// The paging page a screen gets for page 1; a failure (the old null-source crash) fails the cast.
private fun StaticResultPagingSource.loadFirst() = runBlocking {
    load(PagingSource.LoadParams.Refresh(key = null, loadSize = 25, placeholdersEnabled = false))
} as PagingSource.LoadResult.Page
