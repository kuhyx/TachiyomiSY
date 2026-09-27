package exh.recs

import androidx.paging.PagingSource
import eu.kanade.tachiyomi.source.model.FilterList
import exh.metadata.metadata.RaisedSearchMetadata
import exh.recs.sources.ComickPagingSource
import exh.recs.sources.StaticResultPagingSource
import exh.recs.sources.rankedResults
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.manga.model.Manga

@RunWith(RobolectricTestRunner::class)
internal class BrowseRecommendsScreenModelTest {
    private val rig = BrowseRecsRig()

    @Before
    fun setUp() = rig.start()

    @After
    fun tearDown() = rig.stop()

    @Test
    fun singleSourcePicksByName() {
        val model = BrowseRecommendsScreenModel(rig.singleArgs(), rig.browse.getManga)
        model.state.value.filterable shouldBe false
        model.source shouldBe rig.browse.source
        val paging = model.createSourcePagingSource("", FilterList())
        paging.shouldBeInstanceOf<ComickPagingSource>().associatedSourceId shouldBe 1L
    }

    @Test
    fun singleSourceLoadsItsPage() {
        val paging = BrowseRecommendsScreenModel(rig.singleArgs()).recommendationSource
        val result = runBlocking {
            paging.load(PagingSource.LoadParams.Refresh(key = null, loadSize = 25, placeholdersEnabled = false))
        }
        val page = result.shouldBeInstanceOf<PagingSource.LoadResult.Page<Long, Pair<Manga, RaisedSearchMetadata?>>>()
        page.data.map { it.first.title } shouldBe listOf("Rec One")
    }

    @Test
    fun mergedResultsUseTheirSource() {
        val args = BrowseRecommendsScreen.Args.MergedSourceMangas(rankedResults(2, associatedSourceId = 1L))
        val model = BrowseRecommendsScreenModel(args)
        model.source shouldBe rig.browse.source
        model.recommendationSource.shouldBeInstanceOf<StaticResultPagingSource>().data shouldBe args.results
    }

    @Test
    fun mergedResultsWithoutASource() {
        every { rig.browse.sourceManager.getOrStub(-1L) } returns rig.browse.source
        every { rig.browse.exhSavedSearch.subscribe(-1L, any()) } returns rig.browse.savedSearches
        val args = BrowseRecommendsScreen.Args.MergedSourceMangas(rankedResults(1, associatedSourceId = null))
        BrowseRecommendsScreenModel(args).createSourcePagingSource("", FilterList())
            .shouldBeInstanceOf<StaticResultPagingSource>()
        verify { rig.browse.sourceManager.getOrStub(-1L) }
    }

    @Test
    fun metadataIsKept() {
        val args = BrowseRecommendsScreen.Args.MergedSourceMangas(rankedResults(1, associatedSourceId = 1L))
        val model = BrowseRecommendsScreenModel(args)
        val manga = Manga.create().copy(id = 9L)
        val metadata = mockk<RaisedSearchMetadata>()
        runBlocking { with(model) { flowOf(manga).combineMetadata(metadata).toList() } } shouldBe
            listOf(manga to metadata)
    }

    @Test
    fun argsAreValues() {
        val single = rig.singleArgs() as BrowseRecommendsScreen.Args.SingleSourceManga
        single.copy(mangaId = 5L).mangaId shouldBe 5L
        single.hashCode() shouldBe single.copy().hashCode()
        single.toString().contains("SingleSourceManga") shouldBe true
        val merged = BrowseRecommendsScreen.Args.MergedSourceMangas(rankedResults(1))
        merged.copy() shouldBe merged
        merged.hashCode() shouldBe merged.copy().hashCode()
        merged.toString().contains("MergedSourceMangas") shouldBe true
    }
}
