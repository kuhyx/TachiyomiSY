package eu.kanade.tachiyomi.ui.library

import eu.kanade.tachiyomi.source.Source
import exh.metadata.sql.models.SearchTag
import exh.metadata.sql.models.SearchTitle
import exh.search.Namespace
import exh.search.QueryComponent
import exh.search.SearchEngine
import exh.search.StringTextComponent
import exh.search.Text
import io.mockk.every
import io.mockk.mockk
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.track.model.Track

/** A [LibrarySearch] over [LibraryHarness] mocks, with shorthands for its query components. */
internal class LibrarySearchRig(val harness: LibraryHarness) {
    val search by lazy {
        LibrarySearch(
            searchEngine = SearchEngine(),
            getIdsOfFavoriteMangaWithMetadata = harness.getIdsWithMetadata,
            getSearchTags = harness.getSearchTags,
            getSearchTitles = harness.getSearchTitles,
            getTracks = harness.getTracks,
            sourceManager = harness.sourceManager,
            trackerManager = harness.trackerManager,
        )
    }

    fun text(value: String, excluded: Boolean = false): Text =
        Text().apply {
            components += StringTextComponent(value)
            this.excluded = excluded
        }

    fun namespace(name: String, tag: String? = null, excluded: Boolean = false): Namespace =
        Namespace(name, tag?.let { text(it) }).apply { this.excluded = excluded }

    fun source(name: String, id: Long = 5): Source {
        val source = mockk<Source>()
        every { source.name } returns name
        every { source.id } returns id
        return source
    }

    fun matches(query: QueryComponent, manga: Manga, extras: SearchExtras = SearchExtras()): Boolean =
        search.filterManga(
            queries = listOf(query),
            libraryManga = libraryItem(manga).libraryManga,
            tracks = extras.tracks,
            source = extras.source,
            checkGenre = true,
            searchTags = extras.tags,
            searchTitles = extras.titles,
            loggedInTrackServices = extras.loggedIn,
        )
}

/** What besides the manga a query can match against. */
internal data class SearchExtras(
    val tracks: List<Track>? = null,
    val source: Source? = null,
    val tags: List<SearchTag>? = null,
    val titles: List<SearchTitle>? = null,
    val loggedIn: Map<Long, TriState> = emptyMap(),
)

internal fun tag(name: String, namespace: String? = null): SearchTag =
    SearchTag(id = null, mangaId = 1, namespace = namespace, name = name, type = 0)

internal fun title(value: String): SearchTitle = SearchTitle(id = null, mangaId = 1, title = value, type = 0)
