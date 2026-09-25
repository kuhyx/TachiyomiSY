package eu.kanade.tachiyomi.ui.browse.feed

import eu.kanade.tachiyomi.source.CatalogueSource
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
import tachiyomi.domain.source.interactor.CountFeedSavedSearchGlobal
import tachiyomi.domain.source.interactor.DeleteFeedSavedSearchById
import tachiyomi.domain.source.interactor.GetFeedSavedSearchGlobal
import tachiyomi.domain.source.interactor.GetSavedSearchBySourceId
import tachiyomi.domain.source.interactor.GetSavedSearchGlobalFeed
import tachiyomi.domain.source.interactor.InsertFeedSavedSearch
import tachiyomi.domain.source.model.FeedSavedSearch
import tachiyomi.domain.source.model.SavedSearch
import tachiyomi.domain.source.service.SourceManager

/** A saved search [id] of source 1 with [filtersJson]. */
internal fun savedSearch(id: Long, filtersJson: String? = null, query: String? = "q"): SavedSearch =
    SavedSearch(id = id, source = 1L, name = "Search $id", query = query, filtersJson = filtersJson)

/** A global feed row [id] of [source], optionally on saved search [savedSearch]. */
internal fun feed(id: Long, source: Long = 1L, savedSearch: Long? = null): FeedSavedSearch =
    FeedSavedSearch(id = id, source = source, savedSearch = savedSearch, global = true)

/** Koin and collaborators for [FeedScreenModel]. */
internal class FeedHarness {
    val koin: BrowseKoin = BrowseKoin()
    val initialized: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val source: CatalogueSource = source(1L, "One", latest = true)
    val sources: MutableList<CatalogueSource> = mutableListOf(source)
    val sourceManager: SourceManager = mockk {
        every { isInitialized } returns initialized
        every { get(any()) } answers { sources.firstOrNull { it.id == firstArg<Long>() } }
        every { getVisibleSources() } answers { sources.toList() }
    }
    val feeds: MutableStateFlow<List<FeedSavedSearch>> = MutableStateFlow(emptyList())
    val getFeeds: GetFeedSavedSearchGlobal = mockk { every { subscribe() } returns feeds }
    val getSavedSearches: GetSavedSearchGlobalFeed = mockk { coEvery { await() } returns emptyList() }
    val count: CountFeedSavedSearchGlobal = mockk { coEvery { await() } returns 0L }
    val bySource: GetSavedSearchBySourceId = mockk { coEvery { await(any()) } returns emptyList() }
    val insert: InsertFeedSavedSearch = mockk(relaxed = true)
    val delete: DeleteFeedSavedSearchById = mockk(relaxed = true)
    val getManga: GetManga = mockk()
    val networkToLocal: NetworkToLocalManga = mockk {
        coEvery { invoke(any<List<Manga>>()) } answers { firstArg() }
    }

    fun source(sourceId: Long, sourceName: String, latest: Boolean = false, sourceLang: String = "en") =
        mockk<CatalogueSource> {
            every { id } returns sourceId
            every { name } returns sourceName
            every { lang } returns sourceLang
            every { supportsLatest } returns latest
            every { getFilterList() } returns FilterList()
            coEvery { getLatestUpdates(1) } returns MangasPage(listOf(title("Latest")), false)
            coEvery { getSearchManga(1, any(), any()) } returns MangasPage(listOf(title("Found")), false)
        }

    private fun title(name: String) = SManga.create().apply {
        url = "/$name"
        title = name
    }

    fun start() = koin.start(
        module {
            single { sourceManager }
            single { getFeeds }
            single { getSavedSearches }
            single { count }
            single { bySource }
            single { insert }
            single { delete }
            single { getManga }
            single { networkToLocal }
        },
    )

    fun stop() = koin.stop()
}
