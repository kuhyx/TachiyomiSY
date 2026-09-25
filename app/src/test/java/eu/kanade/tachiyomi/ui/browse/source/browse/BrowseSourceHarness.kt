package eu.kanade.tachiyomi.ui.browse.source.browse

import eu.kanade.domain.manga.interactor.UpdateManga
import eu.kanade.domain.source.interactor.GetExhSavedSearch
import eu.kanade.domain.source.interactor.GetIncognitoState
import eu.kanade.domain.track.interactor.AddTracks
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.ui.browse.BrowseKoin
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.dsl.module
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.interactor.SetMangaCategories
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.chapter.interactor.SetMangaDefaultChapterFlags
import tachiyomi.domain.manga.interactor.GetDuplicateLibraryManga
import tachiyomi.domain.manga.interactor.GetFlatMetadataById
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.source.interactor.DeleteSavedSearchById
import tachiyomi.domain.source.interactor.GetRemoteManga
import tachiyomi.domain.source.interactor.InsertSavedSearch
import tachiyomi.domain.source.model.EXHSavedSearch
import tachiyomi.domain.source.service.SourceManager

/** A genre group with an "Action" tri-state and a "Comedy" checkbox, plus a "Mode" select. */
internal fun genreFilters(): FilterList = FilterList(
    object : Filter.Group<Filter<*>>(
        "Genres",
        listOf(
            object : Filter.TriState("Action") {},
            object : Filter.CheckBox("Comedy") {},
            object : Filter.Text("Odd") {},
        ),
    ) {},
    object : Filter.Select<String>("Mode", arrayOf("Any", "Oneshot")) {},
    object : Filter.Header("Note") {},
)

/** Koin and collaborators for [BrowseSourceScreenModel] over source 1. */
internal class BrowseSourceHarness {
    val koin: BrowseKoin = BrowseKoin()
    var filters: () -> FilterList = { FilterList() }
    val source: CatalogueSource = mockk(relaxed = true) {
        every { id } returns 1L
        every { name } returns "One"
        every { getFilterList() } answers { filters() }
    }
    val sourceManager: SourceManager = mockk { every { getOrStub(1L) } returns source }
    val coverCache: CoverCache = mockk(relaxed = true)
    val getRemoteManga: GetRemoteManga = mockk(relaxed = true)
    val getDuplicates: GetDuplicateLibraryManga = mockk()
    val categories: MutableStateFlow<List<Category>> = MutableStateFlow(emptyList())
    val getCategories: GetCategories = mockk {
        every { subscribe() } returns categories
        coEvery { await(any<Long>()) } returns emptyList()
    }
    val setMangaCategories: SetMangaCategories = mockk(relaxed = true)
    val setDefaultFlags: SetMangaDefaultChapterFlags = mockk(relaxed = true)
    val getManga: GetManga = mockk(relaxed = true)
    val updateManga: UpdateManga = mockk(relaxed = true)
    val addTracks: AddTracks = mockk(relaxed = true)
    val incognito: GetIncognitoState = mockk { every { await(any()) } returns false }
    val getFlatMetadata: GetFlatMetadataById = mockk(relaxed = true)
    val deleteSavedSearch: DeleteSavedSearchById = mockk(relaxed = true)
    val insertSavedSearch: InsertSavedSearch = mockk(relaxed = true)
    val savedSearches: MutableStateFlow<List<EXHSavedSearch>> = MutableStateFlow(emptyList())
    val exhSavedSearch: GetExhSavedSearch = mockk {
        every { subscribe(1L, any()) } returns savedSearches
        coEvery { awaitOne(any(), any()) } returns null
    }

    init {
        coEvery { getDuplicates(any()) } returns emptyList()
    }

    fun start() = koin.start(
        module {
            single { sourceManager }
            single { coverCache }
            single { getRemoteManga }
            single { getDuplicates }
            single { getCategories }
            single { setMangaCategories }
            single { setDefaultFlags }
            single { getManga }
            single { updateManga }
            single { addTracks }
            single { incognito }
            single { getFlatMetadata }
            single { deleteSavedSearch }
            single { insertSavedSearch }
            single { exhSavedSearch }
        },
    )

    fun stop() = koin.stop()

    fun model(
        listing: String? = GetRemoteManga.QUERY_POPULAR,
        filtersJson: String? = null,
        savedSearch: Long? = null,
    ): BrowseSourceScreenModel = BrowseSourceScreenModel(
        sourceId = 1L,
        listingQuery = listing,
        filtersJson = filtersJson,
        savedSearch = savedSearch,
    )
}
