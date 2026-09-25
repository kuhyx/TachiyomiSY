package eu.kanade.tachiyomi.ui.browse.source.feed

import eu.kanade.domain.source.interactor.GetExhSavedSearch
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.ui.browse.BrowseKoin
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.dsl.module
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.interactor.CountFeedSavedSearchBySourceId
import tachiyomi.domain.source.interactor.DeleteFeedSavedSearchById
import tachiyomi.domain.source.interactor.GetFeedSavedSearchBySourceId
import tachiyomi.domain.source.interactor.GetSavedSearchBySourceIdFeed
import tachiyomi.domain.source.interactor.InsertFeedSavedSearch
import tachiyomi.domain.source.model.EXHSavedSearch
import tachiyomi.domain.source.model.FeedSavedSearch
import tachiyomi.domain.source.service.SourceManager

/** One filter whose state tells a changed filter list from the source's default one. */
internal class Flag(state: Boolean = false) : Filter.CheckBox("Flag", state) {
    override fun equals(other: Any?): Boolean = other is Flag && other.state == state

    override fun hashCode(): Int = state.hashCode()
}

/** Koin and collaborators for [SourceFeedScreenModel] over source 1. */
internal class SourceFeedHarness {
    val koin: BrowseKoin = BrowseKoin()
    var latest: Boolean = true
    val source: CatalogueSource = mockk {
        every { id } returns 1L
        every { name } returns "One"
        every { lang } returns "en"
        every { supportsLatest } answers { latest }
        every { getFilterList() } answers { FilterList(Flag()) }
        coEvery { getPopularManga(1) } returns page("Popular")
        coEvery { getLatestUpdates(1) } returns page("Latest")
        coEvery { getSearchManga(1, any(), any()) } returns page("Found")
    }
    val sourceManager: SourceManager = mockk { every { getOrStub(1L) } returns source }
    val feeds: MutableStateFlow<List<FeedSavedSearch>> = MutableStateFlow(emptyList())
    val getFeeds: GetFeedSavedSearchBySourceId = mockk { every { subscribe(1L) } returns feeds }
    val getSearches: GetSavedSearchBySourceIdFeed = mockk { coEvery { await(1L) } returns emptyList() }
    val count: CountFeedSavedSearchBySourceId = mockk { coEvery { await(1L) } returns 0L }
    val insert: InsertFeedSavedSearch = mockk(relaxed = true)
    val delete: DeleteFeedSavedSearchById = mockk(relaxed = true)
    val exhSearches: GetExhSavedSearch = mockk {
        coEvery { await(1L, any()) } returns listOf(search(2L, "b"), search(3L, "A"))
    }
    val getManga: GetManga = mockk()
    val networkToLocal: NetworkToLocalManga = mockk()

    init {
        coEvery { networkToLocal(any<List<Manga>>()) } answers { firstArg() }
    }

    fun search(searchId: Long, searchName: String, filters: FilterList? = FilterList(Flag(true))) =
        EXHSavedSearch(id = searchId, name = searchName, query = "q", filterList = filters)

    private fun page(title: String) = MangasPage(
        listOf(
            SManga.create().apply {
                url = "/$title"
                this.title = title
            },
        ),
        false,
    )

    fun start() = koin.start(
        module {
            single { sourceManager }
            single { getFeeds }
            single { getSearches }
            single { count }
            single { insert }
            single { delete }
            single { exhSearches }
            single { getManga }
            single { networkToLocal }
        },
    )

    fun stop() = koin.stop()
}
