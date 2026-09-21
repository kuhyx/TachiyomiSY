package eu.kanade.presentation.library

import androidx.compose.runtime.getValue
import dev.icerock.moko.resources.StringResource
import tachiyomi.domain.library.model.LibrarySort
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR

internal fun sortOptions(hasTrackers: Boolean, hasSortTags: Boolean): List<Pair<StringResource, LibrarySort.Type>> {
    val trackerMeanPair =
        if (hasTrackers) MR.strings.action_sort_tracker_score to LibrarySort.Type.TrackerMean else null
    // SY -->
    val tagSortPair = if (hasSortTags) SYMR.strings.tag_sorting to LibrarySort.Type.TagList else null
    // SY <--
    return listOfNotNull(
        MR.strings.action_sort_alpha to LibrarySort.Type.Alphabetical,
        MR.strings.action_sort_total to LibrarySort.Type.TotalChapters,
        MR.strings.action_sort_last_read to LibrarySort.Type.LastRead,
        MR.strings.action_sort_last_manga_update to LibrarySort.Type.LastUpdate,
        MR.strings.action_sort_unread_count to LibrarySort.Type.UnreadCount,
        MR.strings.action_sort_latest_chapter to LibrarySort.Type.LatestChapter,
        MR.strings.action_sort_chapter_fetch_date to LibrarySort.Type.ChapterFetchDate,
        MR.strings.action_sort_date_added to LibrarySort.Type.DateAdded,
        trackerMeanPair,
        // SY -->
        tagSortPair,
        // SY <--
        MR.strings.action_sort_random to LibrarySort.Type.Random,
    )
}

// Tapping the active sort flips its direction; tapping another keeps the current direction.
internal fun nextDirection(isTogglingDirection: Boolean, sortDescending: Boolean): LibrarySort.Direction {
    val descending = if (isTogglingDirection) !sortDescending else sortDescending
    return if (descending) LibrarySort.Direction.Descending else LibrarySort.Direction.Ascending
}
