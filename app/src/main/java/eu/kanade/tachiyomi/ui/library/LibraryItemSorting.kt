package eu.kanade.tachiyomi.ui.library

import eu.kanade.tachiyomi.data.track.TrackerManager
import tachiyomi.core.common.util.lang.compareToWithCollator
import tachiyomi.domain.library.model.LibrarySort
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.track.model.Track

// An entry with no tracker score sorts below every scored one.
private const val DEFAULT_TRACKER_SCORE = -1.0

internal val ALPHABETICALLY: Comparator<LibraryItem> = Comparator { manga1, manga2 ->
    val title1 = manga1.libraryManga.manga.title.lowercase()
    val title2 = manga2.libraryManga.manga.title.lowercase()
    title1.compareToWithCollator(title2)
}

/**
 * The comparator behind one sort type, before direction is applied. [trackerScores] and [sortTags] are only
 * read by the types that need them, so an expensive lookup is never paid for a sort that ignores it.
 */
internal fun LibrarySort.Type.comparator(
    ascending: Boolean,
    trackerScores: Lazy<Map<Long, Double?>>,
    sortTags: Lazy<List<String>>,
): Comparator<LibraryItem> = when (this) {
    LibrarySort.Type.Alphabetical -> ALPHABETICALLY
    LibrarySort.Type.LastRead -> compareBy { it.libraryManga.lastRead }
    LibrarySort.Type.LastUpdate -> compareBy { it.libraryManga.manga.lastUpdate }
    LibrarySort.Type.UnreadCount -> unreadCountComparator(ascending)
    LibrarySort.Type.TotalChapters -> compareBy { it.libraryManga.totalChapters }
    LibrarySort.Type.LatestChapter -> compareBy { it.libraryManga.latestUpload }
    LibrarySort.Type.ChapterFetchDate -> compareBy { it.libraryManga.chapterFetchedAt }
    LibrarySort.Type.DateAdded -> compareBy { it.libraryManga.manga.dateAdded }
    LibrarySort.Type.TrackerMean -> compareBy { trackerScores.value[it.id] ?: DEFAULT_TRACKER_SCORE }
    LibrarySort.Type.Random -> error("Why Are We Still Here? Just To Suffer?")
    // SY -->
    LibrarySort.Type.TagList -> compareBy { item ->
        sortTags.value.indexOfFirst { item.libraryManga.manga.genre?.contains(it) ?: false }
    }
    // SY <--
}

// Ensure unread content comes first whichever way the count is sorted.
private fun unreadCountComparator(ascending: Boolean): Comparator<LibraryItem> = Comparator { manga1, manga2 ->
    val unread1 = manga1.libraryManga.unreadCount
    val unread2 = manga2.libraryManga.unreadCount
    when {
        unread1 == unread2 -> 0
        unread1 == 0L -> if (ascending) 1 else -1
        unread2 == 0L -> if (ascending) -1 else 1
        else -> unread1.compareTo(unread2)
    }
}

// SY --> The sort tags in their configured order, from "order|tag" preference entries.
internal fun LibraryPreferences.sortTagList(): List<String> = sortTagsForLibrary.get()
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
// SY <--

// Each entry's mean 10-point score across the logged-in trackers; null when it has no tracks at all.
internal fun TrackerManager.meanScores(
    trackMap: Map<Long, List<Track>>,
    loggedInTrackerIds: Set<Long>,
): Map<Long, Double?> {
    val trackerMap = getAll(loggedInTrackerIds).associateBy { e -> e.id }
    return trackMap.mapValues { entry ->
        entry.value.ifEmpty { null }?.mapNotNull { trackerMap[it.trackerId]?.get10PointScore(it) }?.average()
    }
}
