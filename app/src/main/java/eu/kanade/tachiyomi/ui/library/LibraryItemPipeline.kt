package eu.kanade.tachiyomi.ui.library

import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastMap
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
import tachiyomi.core.common.util.lang.compareToWithCollator
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
        val downloadedOnly = preferences.globalFilterDownloaded
        val skipOutsideReleasePeriod = preferences.skipOutsideReleasePeriod
        val filterDownloaded = if (downloadedOnly) TriState.ENABLED_IS else preferences.filterDownloaded
        val filterUnread = preferences.filterUnread
        val filterStarted = preferences.filterStarted
        val filterBookmarked = preferences.filterBookmarked
        val filterCompleted = preferences.filterCompleted
        val filterIntervalCustom = preferences.filterIntervalCustom

        val isNotLoggedInAnyTrack = trackingFilter.isEmpty()

        val excludedTracks = trackingFilter.mapNotNull { if (it.value == TriState.ENABLED_NOT) it.key else null }
        val includedTracks = trackingFilter.mapNotNull { if (it.value == TriState.ENABLED_IS) it.key else null }
        val trackFiltersIsIgnored = includedTracks.isEmpty() && excludedTracks.isEmpty()

        // SY -->
        val filterLewd = preferences.filterLewd
        // SY <--

        val filterFnDownloaded: (LibraryItem) -> Boolean = {
            applyFilter(filterDownloaded) { it.isLocal || it.downloadCount > 0 }
        }

        val filterFnUnread: (LibraryItem) -> Boolean = {
            applyFilter(filterUnread) { it.libraryManga.unreadCount > 0 }
        }

        val filterFnStarted: (LibraryItem) -> Boolean = {
            applyFilter(filterStarted) { it.libraryManga.hasStarted }
        }

        val filterFnBookmarked: (LibraryItem) -> Boolean = {
            applyFilter(filterBookmarked) { it.libraryManga.hasBookmarks }
        }

        val filterFnCompleted: (LibraryItem) -> Boolean = {
            applyFilter(filterCompleted) { it.libraryManga.manga.status.toInt() == SManga.COMPLETED }
        }

        val filterFnIntervalCustom: (LibraryItem) -> Boolean = {
            if (skipOutsideReleasePeriod) {
                applyFilter(filterIntervalCustom) { it.libraryManga.manga.fetchInterval < 0 }
            } else {
                true
            }
        }

        // SY -->
        val filterFnLewd: (LibraryItem) -> Boolean = {
            applyFilter(filterLewd) { it.libraryManga.manga.isLewd() }
        }
        // SY <--

        val filterFnTracking: (LibraryItem) -> Boolean = tracking@{ item ->
            if (isNotLoggedInAnyTrack || trackFiltersIsIgnored) {
                true
            } else {
                val mangaTracks = trackMap[item.id].orEmpty().map { it.trackerId }

                val isExcluded = excludedTracks.isNotEmpty() && mangaTracks.fastAny { it in excludedTracks }
                val isIncluded = includedTracks.isEmpty() || mangaTracks.fastAny { it in includedTracks }

                !isExcluded && isIncluded
            }
        }

        return fastFilter {
            filterFnDownloaded(it) &&
                filterFnUnread(it) &&
                filterFnStarted(it) &&
                filterFnBookmarked(it) &&
                filterFnCompleted(it) &&
                filterFnIntervalCustom(it) &&
                filterFnTracking(it) &&
                // SY -->
                filterFnLewd(it)
            // SY <--
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
        when (groupType) {
            LibraryGroup.BY_DEFAULT -> {
                // SY <--
                val groupCache = mutableMapOf</* Category.id */ Long, MutableList</* LibraryItem */ Long>>()
                forEach { item ->
                    item.libraryManga.categories.forEach { categoryId ->
                        groupCache.getOrPut(categoryId) { mutableListOf() }.add(item.id)
                    }
                }

                return categories.filter { showSystemCategory || !it.isSystemCategory }
                    .associateWith { groupCache[it.id]?.toList().orEmpty() }
            }
            // SY -->
            LibraryGroup.UNGROUPED -> {
                return mapOf(
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
                return getGroupedMangaItems(
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
        val listOfTags by lazy {
            libraryPreferences.sortTagsForLibrary.get()
                .asSequence()
                .mapNotNull {
                    val list = it.split("|")
                    val order = list.getOrNull(0)?.toIntOrNull()
                    val tag = list.getOrNull(1)
                    if (order != null && tag != null) order to tag else null
                }
                .sortedBy { it.first }
                .map { it.second }
                .toList()
        }
        // SY <--

        val sortAlphabetically: (LibraryItem, LibraryItem) -> Int = { manga1, manga2 ->
            val title1 = manga1.libraryManga.manga.title.lowercase()
            val title2 = manga2.libraryManga.manga.title.lowercase()
            title1.compareToWithCollator(title2)
        }

        val defaultTrackerScoreSortValue = -1.0
        val trackerScores by lazy {
            val trackerMap = trackerManager.getAll(loggedInTrackerIds).associateBy { e -> e.id }
            trackMap.mapValues { entry ->
                if (entry.value.isEmpty()) {
                    null
                } else {
                    entry.value
                        .mapNotNull { trackerMap[it.trackerId]?.get10PointScore(it) }
                        .average()
                }
            }
        }

        fun LibrarySort.comparator(): Comparator<LibraryItem> = Comparator { manga1, manga2 ->
            // SY -->
            val sort = groupSort ?: this
            // SY <--
            when (sort.type) {
                LibrarySort.Type.Alphabetical -> {
                    sortAlphabetically(manga1, manga2)
                }

                LibrarySort.Type.LastRead -> {
                    manga1.libraryManga.lastRead.compareTo(manga2.libraryManga.lastRead)
                }

                LibrarySort.Type.LastUpdate -> {
                    manga1.libraryManga.manga.lastUpdate.compareTo(manga2.libraryManga.manga.lastUpdate)
                }

                LibrarySort.Type.UnreadCount -> {
                    when {
                        // Ensure unread content comes first
                        manga1.libraryManga.unreadCount == manga2.libraryManga.unreadCount -> 0
                        manga1.libraryManga.unreadCount == 0L -> if (sort.isAscending) 1 else -1
                        manga2.libraryManga.unreadCount == 0L -> if (sort.isAscending) -1 else 1
                        else -> manga1.libraryManga.unreadCount.compareTo(manga2.libraryManga.unreadCount)
                    }
                }

                LibrarySort.Type.TotalChapters -> {
                    manga1.libraryManga.totalChapters.compareTo(manga2.libraryManga.totalChapters)
                }

                LibrarySort.Type.LatestChapter -> {
                    manga1.libraryManga.latestUpload.compareTo(manga2.libraryManga.latestUpload)
                }

                LibrarySort.Type.ChapterFetchDate -> {
                    manga1.libraryManga.chapterFetchedAt.compareTo(manga2.libraryManga.chapterFetchedAt)
                }

                LibrarySort.Type.DateAdded -> {
                    manga1.libraryManga.manga.dateAdded.compareTo(manga2.libraryManga.manga.dateAdded)
                }

                LibrarySort.Type.TrackerMean -> {
                    val item1Score = trackerScores[manga1.id] ?: defaultTrackerScoreSortValue
                    val item2Score = trackerScores[manga2.id] ?: defaultTrackerScoreSortValue
                    item1Score.compareTo(item2Score)
                }

                LibrarySort.Type.Random -> {
                    error("Why Are We Still Here? Just To Suffer?")
                }
                // SY -->
                LibrarySort.Type.TagList -> {
                    val manga1IndexOfTag = listOfTags.indexOfFirst {
                        manga1.libraryManga.manga.genre?.contains(it) ?: false
                    }
                    val manga2IndexOfTag = listOfTags.indexOfFirst {
                        manga2.libraryManga.manga.genre?.contains(it) ?: false
                    }
                    manga1IndexOfTag.compareTo(manga2IndexOfTag)
                }
                // SY <--
            }
        }

        return mapValues { (key, value) ->
            // SY -->
            val sort = groupSort ?: key.sort
            if (sort.type == LibrarySort.Type.Random) {
                // SY <--
                value.shuffled(Random(libraryPreferences.randomSortSeed.get()))
            } else {
                val manga = value.mapNotNull { favoritesById[it] }

                // SY -->
                val comparator = sort.comparator()
                    // SY <--
                    .let { if (/* SY --> */ sort.isAscending /* SY <-- */) it else it.reversed() }
                    .thenComparator(sortAlphabetically)

                manga.sortedWith(comparator).map { it.id }
            }
        }
    }

    private fun List<LibraryItem>.getGroupedMangaItems(
        groupType: Int,
    ): Map<Category, List</* LibraryItem */ Long>> {
        val context = preferences.context
        return when (groupType) {
            LibraryGroup.BY_TRACK_STATUS -> {
                val tracks = runBlocking { getTracks.await() }.groupBy { it.mangaId }
                groupBy { item ->
                    val status = tracks[item.libraryManga.manga.id]?.firstNotNullOfOrNull { track ->
                        TrackStatus.parseTrackerStatus(trackerManager, track.trackerId, track.status)
                    } ?: TrackStatus.OTHER

                    status.int
                }.mapKeys { (id) ->
                    Category(
                        id = id.toLong(),
                        name = TrackStatus.entries
                            .find { it.int == id }
                            .let { it ?: TrackStatus.OTHER }
                            .let { context.stringResource(it.res) },
                        order = TrackStatus.entries.indexOfFirst {
                            it.int == id
                        }.takeUnless { it == -1 }?.toLong() ?: TrackStatus.OTHER.ordinal.toLong(),
                        flags = 0,
                    )
                }
            }

            LibraryGroup.BY_SOURCE -> {
                val sources: List<Long>
                groupBy { item ->
                    item.libraryManga.manga.source
                }.also {
                    sources = it.keys
                        .map {
                            sourceManager.getOrStub(it)
                        }
                        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name.ifBlank { it.id.toString() } })
                        .map { it.id }
                }.mapKeys {
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

            LibraryGroup.BY_STATUS -> {
                groupBy { item ->
                    item.libraryManga.manga.status
                }.mapKeys {
                    Category(
                        id = it.key + 1,
                        name = when (it.key) {
                            SManga.ONGOING.toLong() -> context.stringResource(MR.strings.ongoing)
                            SManga.LICENSED.toLong() -> context.stringResource(MR.strings.licensed)
                            SManga.CANCELLED.toLong() -> context.stringResource(MR.strings.cancelled)
                            SManga.ON_HIATUS.toLong() -> context.stringResource(MR.strings.on_hiatus)
                            SManga.PUBLISHING_FINISHED.toLong() ->
                                context.stringResource(MR.strings.publishing_finished)
                            SManga.COMPLETED.toLong() -> context.stringResource(MR.strings.completed)
                            else -> context.stringResource(MR.strings.unknown)
                        },
                        order = when (it.key) {
                            SManga.ONGOING.toLong() -> 1
                            SManga.LICENSED.toLong() -> 2
                            SManga.CANCELLED.toLong() -> 3
                            SManga.ON_HIATUS.toLong() -> 4
                            SManga.PUBLISHING_FINISHED.toLong() -> 5
                            SManga.COMPLETED.toLong() -> 6
                            else -> 7
                        },
                        flags = 0,
                    )
                }
            }

            else -> {
                emptyMap()
            }
        }.toSortedMap(compareBy { it.order })
            .mapValues { (_, libraryItem) -> libraryItem.fastMap { it.id } }
    }
}
