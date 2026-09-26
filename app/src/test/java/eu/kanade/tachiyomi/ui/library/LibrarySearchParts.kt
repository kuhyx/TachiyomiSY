package eu.kanade.tachiyomi.ui.library

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import eu.kanade.tachiyomi.data.track.BaseTracker
import eu.kanade.tachiyomi.data.track.TrackerManager
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
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.interactor.GetIdsOfFavoriteMangaWithMetadata
import tachiyomi.domain.manga.interactor.GetSearchTags
import tachiyomi.domain.manga.interactor.GetSearchTitles
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.model.Track
import tachiyomi.i18n.MR
import tachiyomi.source.local.LocalSource

/** A text query component for [query]; an empty [query] has no components and reads as blank. */
internal fun text(query: String, excluded: Boolean = false): Text = Text().apply {
    if (query.isNotEmpty()) components += StringTextComponent(query)
    this.excluded = excluded
}

internal fun namespace(name: String, tag: String? = null, excluded: Boolean = false): Namespace =
    Namespace(name, tag?.let { text(it) }).apply { this.excluded = excluded }

internal fun searchTag(namespace: String?, name: String): SearchTag =
    SearchTag(id = null, mangaId = 1L, namespace = namespace, name = name, type = 0)

/**
 * A [LibrarySearch] over mocks: tracker 5 ("Trk", every status "Reading") is the only one known, and
 * source 1 is "Src". [start] registers the Application its search targets pull from Injekt.
 */
internal class LibrarySearchParts {
    val app: Application = ApplicationProvider.getApplicationContext()
    val tracker: BaseTracker = mockk {
        every { name } returns "Trk"
        every { getStatus(any()) } returns null
        every { getStatus(1L) } returns MR.strings.reading
    }
    val trackerManager: TrackerManager = mockk<TrackerManager>().also {
        every { it.get(any()) } returns null
        every { it.get(5L) } returns tracker
    }
    val source: Source = mockk {
        every { id } returns 1L
        every { name } returns "Src"
    }
    val sourceManager: SourceManager = mockk<SourceManager>().also {
        every { it.get(any()) } returns null
        every { it.get(1L) } returns source
    }
    val getIds: GetIdsOfFavoriteMangaWithMetadata = mockk()
    val getSearchTags: GetSearchTags = mockk()
    val getSearchTitles: GetSearchTitles = mockk()
    val getTracks: GetTracks = mockk()
    val search: LibrarySearch = LibrarySearch(
        searchEngine = SearchEngine(),
        getIdsOfFavoriteMangaWithMetadata = getIds,
        getSearchTags = getSearchTags,
        getSearchTitles = getSearchTitles,
        getTracks = getTracks,
        sourceManager = sourceManager,
        trackerManager = trackerManager,
    )
    val loggedIn: Map<Long, TriState> = mapOf(5L to TriState.DISABLED)

    /** An entry with every searchable field set: "Needle" by "Au"/"Ar", described "De", genre "Ge". */
    val full: LibraryManga = libEntry(
        libManga(1L, title = "Needle").copy(
            ogAuthor = "Au",
            ogArtist = "Ar",
            ogDescription = "De",
            ogGenre = listOf("Ge"),
        ),
    )
    val tracks: List<Track> = listOf(track(mangaId = 1L, trackerId = 5L, status = 1L))
    val tags: List<SearchTag> = listOf(searchTag("ns", "Tg"))
    val titles: List<SearchTitle> = listOf(SearchTitle(id = null, mangaId = 1L, title = "Alt", type = 0))

    fun start() {
        stopKoin()
        startKoin {
            modules(
                module {
                    single { app }
                    single { SearchEngine() }
                    single { getIds }
                    single { getSearchTags }
                    single { getSearchTitles }
                    single { getTracks }
                    single { sourceManager }
                    single { trackerManager }
                },
            )
        }
    }

    fun stop() {
        stopKoin()
    }

    /** Whether [full] (with all its tracks, tags and titles) passes [query]. */
    fun fullPasses(query: QueryComponent): Boolean = search.filterManga(
        queries = listOf(query),
        libraryManga = full,
        tracks = tracks,
        source = source,
        searchTags = tags,
        searchTitles = titles,
        loggedInTrackServices = loggedIn,
    )

    /** Whether an entry with nothing but a title passes [query]; [local] puts it in the local source. */
    fun barePasses(query: QueryComponent, local: Boolean = false): Boolean = search.filterManga(
        queries = listOf(query),
        libraryManga = libEntry(libManga(2L, title = "Bare", source = if (local) LocalSource.ID else 3L)),
        tracks = null,
        source = null,
        loggedInTrackServices = emptyMap(),
    )
}
