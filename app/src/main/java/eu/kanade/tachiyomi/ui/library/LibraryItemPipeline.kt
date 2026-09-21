package eu.kanade.tachiyomi.ui.library

import android.content.Context
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastMap
import dev.icerock.moko.resources.StringResource
import eu.kanade.domain.base.BasePreferences
import eu.kanade.tachiyomi.data.track.TrackStatus
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.source.model.SManga
import exh.util.isLewd
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.library.model.LibraryGroup
import tachiyomi.domain.library.model.LibrarySort
import tachiyomi.domain.library.model.sort
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.model.applyFilter
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.model.Track
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.source.local.LocalSource
import tachiyomi.source.local.isLocal
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.random.Random

/**
 * The library list pipeline: the filter, grouping and sort passes that turn the favourites into
 * the per-category item lists the screen shows. Pure list transforms, composed by
 * [LibraryScreenModel]; call them with the pipeline as receiver.
 */
internal class LibraryItemPipeline(
    private val preferences: BasePreferences,
    private val libraryPreferences: LibraryPreferences,
    private val trackerManager: TrackerManager = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    private val getTracks: GetTracks = Injekt.get(),
) {
    fun List<LibraryItem>.applyFilters(
        trackMap: Map<Long, List<Track>>,
        trackingFilter: Map<Long, TriState>,
        preferences: LibraryScreenModel.ItemPreferences,
    ): List<LibraryItem> {
        val filterDownloaded =
            if (preferences.globalFilterDownloaded) TriState.ENABLED_IS else preferences.filterDownloaded
        val filters: List<(LibraryItem) -> Boolean> = listOf(
            { applyFilter(filterDownloaded) { it.isLocal || it.downloadCount > 0 } },
            { applyFilter(preferences.filterUnread) { it.libraryManga.unreadCount > 0 } },
            { applyFilter(preferences.filterStarted) { it.libraryManga.hasStarted } },
            { applyFilter(preferences.filterBookmarked) { it.libraryManga.hasBookmarks } },
            { applyFilter(preferences.filterCompleted) { it.libraryManga.manga.status.toInt() == SManga.COMPLETED } },
            {
                !preferences.skipOutsideReleasePeriod ||
                    applyFilter(preferences.filterIntervalCustom) { it.libraryManga.manga.fetchInterval < 0 }
            },
            trackingFilter(trackMap, trackingFilter),
            // SY -->
            { applyFilter(preferences.filterLewd) { it.libraryManga.manga.isLewd() } },
            // SY <--
        )
        return fastFilter { item -> filters.all { it(item) } }
    }

    // Keeps items with an included tracker (when any is chosen) and without an excluded one.
    private fun trackingFilter(
        trackMap: Map<Long, List<Track>>,
        trackingFilter: Map<Long, TriState>,
    ): (LibraryItem) -> Boolean {
        val excludedTracks = trackingFilter.mapNotNull { if (it.value == TriState.ENABLED_NOT) it.key else null }
        val includedTracks = trackingFilter.mapNotNull { if (it.value == TriState.ENABLED_IS) it.key else null }
        // Not logged in anywhere, or no tracker chosen either way: nothing to filter on.
        if (trackingFilter.isEmpty() || (includedTracks.isEmpty() && excludedTracks.isEmpty())) return { true }
        return { item ->
            val mangaTracks = trackMap[item.id].orEmpty().map { it.trackerId }
            val isExcluded = excludedTracks.isNotEmpty() && mangaTracks.fastAny { it in excludedTracks }
            val isIncluded = includedTracks.isEmpty() || mangaTracks.fastAny { it in includedTracks }
            !isExcluded && isIncluded
        }
    }

    fun List<LibraryItem>.applyGrouping(
        categories: List<Category>,
        showSystemCategory: Boolean,
        // SY -->
        groupType: Int,
        // <-- SY
    ): Map<Category, List</* LibraryItem */ Long>> {
        // SY -->
        return when (groupType) {
            LibraryGroup.BY_DEFAULT -> {
                // SY <--
                val groupCache = mutableMapOf</* Category.id */ Long, MutableList</* LibraryItem */ Long>>()
                forEach { item ->
                    item.libraryManga.categories.forEach { categoryId ->
                        groupCache.getOrPut(categoryId) { mutableListOf() }.add(item.id)
                    }
                }

                categories.filter { showSystemCategory || !it.isSystemCategory }
                    .associateWith { groupCache[it.id]?.toList().orEmpty() }
            }
            // SY -->
            LibraryGroup.UNGROUPED -> {
                mapOf(
                    Category(
                        0,
                        preferences.context.stringResource(SYMR.strings.ungrouped),
                        0,
                        0,
                    ) to
                        map { it.id },
                )
            }

            else -> {
                getGroupedMangaItems(
                    groupType = groupType,
                )
            }
        }
        // SY <--
    }

    fun Map<Category, List</* LibraryItem */ Long>>.applySort(
        favoritesById: Map<Long, LibraryItem>,
        trackMap: Map<Long, List<Track>>,
        loggedInTrackerIds: Set<Long>,
        // SY -->
        groupSort: LibrarySort? = null,
        // SY <--
    ): Map<Category, List</* LibraryItem */ Long>> {
        // SY -->
        val listOfTags = lazy { libraryPreferences.sortTagList() }
        // SY <--
        val trackerScores = lazy { trackerManager.meanScores(trackMap, loggedInTrackerIds) }

        return mapValues { (key, value) ->
            // SY -->
            val sort = groupSort ?: key.sort
            if (sort.type == LibrarySort.Type.Random) {
                // SY <--
                value.shuffled(Random(libraryPreferences.randomSortSeed.get()))
            } else {
                val manga = value.mapNotNull { favoritesById[it] }

                val comparator = sort.type.comparator(sort.isAscending, trackerScores, listOfTags)
                    .let { if (/* SY --> */ sort.isAscending /* SY <-- */) it else it.reversed() }
                    .then(ALPHABETICALLY)

                manga.sortedWith(comparator).map { it.id }
            }
        }
    }

    private fun List<LibraryItem>.getGroupedMangaItems(
        groupType: Int,
    ): Map<Category, List</* LibraryItem */ Long>> {
        val context = preferences.context
        val grouped = when (groupType) {
            LibraryGroup.BY_TRACK_STATUS -> groupByTrackStatus(context)
            LibraryGroup.BY_SOURCE -> groupBySource(context)
            LibraryGroup.BY_STATUS -> groupByStatus(context)
            else -> emptyMap()
        }
        return grouped.toSortedMap(compareBy { it.order })
            .mapValues { (_, libraryItem) -> libraryItem.fastMap { it.id } }
    }

    private fun List<LibraryItem>.groupByTrackStatus(context: Context): Map<Category, List<LibraryItem>> {
        val tracks = runBlocking { getTracks.await() }.groupBy { it.mangaId }
        return groupBy { item ->
            val status = tracks[item.libraryManga.manga.id]?.firstNotNullOfOrNull { track ->
                TrackStatus.parseTrackerStatus(trackerManager, track.trackerId, track.status)
            } ?: TrackStatus.OTHER
            status.int
        }.mapKeys { (id) ->
            val status = TrackStatus.entries.find { it.int == id } ?: TrackStatus.OTHER
            Category(
                id = id.toLong(),
                name = context.stringResource(status.res),
                order = TrackStatus.entries.indexOf(status).toLong(),
                flags = 0,
            )
        }
    }

    private fun List<LibraryItem>.groupBySource(context: Context): Map<Category, List<LibraryItem>> {
        val bySource = groupBy { item -> item.libraryManga.manga.source }
        val sources = bySource.keys
            .map { sourceManager.getOrStub(it) }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name.ifBlank { it.id.toString() } })
            .map { it.id }
        return bySource.mapKeys {
            Category(
                id = it.key,
                name = if (it.key == LocalSource.ID) {
                    context.stringResource(MR.strings.local_source)
                } else {
                    val source = sourceManager.getOrStub(it.key)
                    source.name.ifBlank { source.id.toString() }
                },
                order = sources.indexOf(it.key).takeUnless { it == -1 }?.toLong() ?: Long.MAX_VALUE,
                flags = 0,
            )
        }
    }

    private fun List<LibraryItem>.groupByStatus(context: Context): Map<Category, List<LibraryItem>> {
        return groupBy { item -> item.libraryManga.manga.status }.mapKeys {
            val index = STATUS_GROUPS.indexOfFirst { (status) -> status == it.key }
            val name = if (index == -1) MR.strings.unknown else STATUS_GROUPS[index].second
            val order = if (index == -1) STATUS_GROUPS.size else index
            Category(id = it.key + 1, name = context.stringResource(name), order = order + 1L, flags = 0)
        }
    }
}

// The publishing-status groups in display order; anything else sorts after them as "unknown".
private val STATUS_GROUPS: List<Pair<Long, StringResource>> = listOf(
    SManga.ONGOING.toLong() to MR.strings.ongoing,
    SManga.LICENSED.toLong() to MR.strings.licensed,
    SManga.CANCELLED.toLong() to MR.strings.cancelled,
    SManga.ON_HIATUS.toLong() to MR.strings.on_hiatus,
    SManga.PUBLISHING_FINISHED.toLong() to MR.strings.publishing_finished,
    SManga.COMPLETED.toLong() to MR.strings.completed,
)
