package eu.kanade.tachiyomi.ui.library

import android.app.Application
import android.content.Context
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastMapNotNull
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.source.Source
import exh.metadata.sql.models.SearchTag
import exh.metadata.sql.models.SearchTitle
import exh.search.Namespace
import exh.search.QueryComponent
import exh.search.SearchEngine
import exh.search.Text
import exh.source.isMetadataSource
import exh.util.cancellable
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.interactor.GetIdsOfFavoriteMangaWithMetadata
import tachiyomi.domain.manga.interactor.GetSearchTags
import tachiyomi.domain.manga.interactor.GetSearchTitles
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.model.Track
import tachiyomi.source.local.LocalSource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * The library search (SY): the query language over titles, tags, sources and tracker state that
 * narrows the filtered favourites. Composed by [LibraryScreenModel].
 */
internal class LibrarySearch(
    private val searchEngine: SearchEngine = Injekt.get(),
    private val getIdsOfFavoriteMangaWithMetadata: GetIdsOfFavoriteMangaWithMetadata = Injekt.get(),
    private val getSearchTags: GetSearchTags = Injekt.get(),
    private val getSearchTitles: GetSearchTitles = Injekt.get(),
    private val getTracks: GetTracks = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    private val trackerManager: TrackerManager = Injekt.get(),
) {
    suspend fun filterLibrary(
        unfiltered: List<LibraryItem>,
        query: String?,
        loggedInTrackServices: Map<Long, TriState>,
    ): List<LibraryItem> {
        return if (unfiltered.isNotEmpty() && !query.isNullOrBlank()) {
            // Prepare filter object
            val parsedQuery = searchEngine.parseQuery(query)
            val mangaWithMetaIds = getIdsOfFavoriteMangaWithMetadata.await()
            val tracks = if (loggedInTrackServices.isNotEmpty()) {
                getTracks.await().groupBy { it.mangaId }
            } else {
                emptyMap()
            }
            val sources = unfiltered
                .distinctBy { it.libraryManga.manga.source }
                .fastMapNotNull { sourceManager.get(it.libraryManga.manga.source) }
                .associateBy { it.id }
            unfiltered.asFlow().cancellable().filter { item ->
                val mangaId = item.libraryManga.manga.id
                if (query.startsWith("id:", true)) {
                    val id = query.substringAfter("id:").toLongOrNull()
                    return@filter mangaId == id
                }
                val sourceId = item.libraryManga.manga.source
                if (isMetadataSource(sourceId)) {
                    if (mangaWithMetaIds.binarySearch(mangaId) < 0) {
                        // No meta? Filter using title
                        filterManga(
                            queries = parsedQuery,
                            libraryManga = item.libraryManga,
                            tracks = tracks[mangaId],
                            source = sources[sourceId],
                            loggedInTrackServices = loggedInTrackServices,
                        )
                    } else {
                        val tags = getSearchTags.await(mangaId)
                        val titles = getSearchTitles.await(mangaId)
                        filterManga(
                            queries = parsedQuery,
                            libraryManga = item.libraryManga,
                            tracks = tracks[mangaId],
                            source = sources[sourceId],
                            checkGenre = false,
                            searchTags = tags,
                            searchTitles = titles,
                            loggedInTrackServices = loggedInTrackServices,
                        )
                    }
                } else {
                    filterManga(
                        queries = parsedQuery,
                        libraryManga = item.libraryManga,
                        tracks = tracks[mangaId],
                        source = sources[sourceId],
                        loggedInTrackServices = loggedInTrackServices,
                    )
                }
            }.toList()
        } else {
            unfiltered
        }
    }

    private fun filterManga(
        queries: List<QueryComponent>,
        libraryManga: LibraryManga,
        tracks: List<Track>?,
        source: Source?,
        checkGenre: Boolean = true,
        searchTags: List<SearchTag>? = null,
        searchTitles: List<SearchTitle>? = null,
        loggedInTrackServices: Map<Long, TriState>,
    ): Boolean {
        val manga = libraryManga.manga
        val sourceIdString = manga.source.takeUnless { it == LocalSource.ID }?.toString()
        val genre = if (checkGenre) manga.genre.orEmpty() else emptyList()
        val context = Injekt.get<Application>()
        return queries.all { queryComponent ->
            when (queryComponent.excluded) {
                false -> when (queryComponent) {
                    is Text -> {
                        val query = queryComponent.asQuery()
                        manga.title.contains(query, true) ||
                            manga.author?.contains(query, true) == true ||
                            manga.artist?.contains(query, true) == true ||
                            manga.description?.contains(query, true) == true ||
                            source?.name?.contains(query, true) == true ||
                            (sourceIdString != null && sourceIdString == query) ||
                            (
                                loggedInTrackServices.isNotEmpty() &&
                                    tracks != null &&
                                    filterTracks(query, tracks, context)
                                ) ||
                            genre.fastAny { it.contains(query, true) } ||
                            searchTags?.fastAny { it.name.contains(query, true) } == true ||
                            searchTitles?.fastAny { it.title.contains(query, true) } == true
                    }

                    is Namespace -> {
                        searchTags != null &&
                            searchTags.fastAny {
                                val tag = queryComponent.tag
                                (
                                    it.namespace.equals(queryComponent.namespace, true) &&
                                        tag?.run { it.name.contains(tag.asQuery(), true) } == true
                                    ) ||
                                    (tag == null && it.namespace.equals(queryComponent.namespace, true))
                            }
                    }

                    else -> {
                        true
                    }
                }

                true -> when (queryComponent) {
                    is Text -> {
                        val query = queryComponent.asQuery()
                        query.isBlank() ||
                            (
                                !manga.title.contains(query, true) &&
                                    manga.author?.contains(query, true) != true &&
                                    manga.artist?.contains(query, true) != true &&
                                    manga.description?.contains(query, true) != true &&
                                    source?.name?.contains(query, true) != true &&
                                    sourceIdString != null && sourceIdString != query &&
                                    (
                                        loggedInTrackServices.isEmpty() ||
                                            tracks == null ||
                                            !filterTracks(query, tracks, context)
                                        ) &&
                                    !genre.fastAny { it.contains(query, true) } &&
                                    searchTags?.fastAny { it.name.contains(query, true) } != true &&
                                    searchTitles?.fastAny { it.title.contains(query, true) } != true
                                )
                    }

                    is Namespace -> {
                        val searchedTag = queryComponent.tag?.asQuery()
                        searchTags == null ||
                            (queryComponent.namespace.isBlank() && searchedTag.isNullOrBlank()) ||
                            searchTags.fastAll { mangaTag ->
                                if (queryComponent.namespace.isBlank() && !searchedTag.isNullOrBlank()) {
                                    !mangaTag.name.contains(searchedTag, true)
                                } else if (searchedTag.isNullOrBlank()) {
                                    mangaTag.namespace == null ||
                                        !mangaTag.namespace.equals(queryComponent.namespace, true)
                                } else if (mangaTag.namespace.isNullOrBlank()) {
                                    true
                                } else {
                                    !mangaTag.name.contains(searchedTag, true) ||
                                        !mangaTag.namespace.equals(queryComponent.namespace, true)
                                }
                            }
                    }

                    else -> {
                        true
                    }
                }
            }
        }
    }

    private fun filterTracks(constraint: String, tracks: List<Track>, context: Context): Boolean {
        return tracks.fastAny { track ->
            val trackService = trackerManager.get(track.trackerId)
            if (trackService != null) {
                val status = trackService.getStatus(track.status)?.let {
                    context.stringResource(it)
                }
                val name = trackerManager.get(track.trackerId)?.name
                status?.contains(constraint, true) == true || name?.contains(constraint, true) == true
            } else {
                false
            }
        }
    }
// SY <--
}
