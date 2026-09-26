package eu.kanade.tachiyomi.data.library

import eu.kanade.tachiyomi.data.track.TrackStatus
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.UpdateStrategy
import exh.util.nullIfBlank
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.domain.library.model.GroupLibraryMode
import tachiyomi.domain.library.model.LibraryGroup
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MANGA_HAS_UNREAD
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MANGA_NON_COMPLETED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MANGA_NON_READ
import tachiyomi.domain.library.service.LibraryPreferences.Companion.MANGA_OUTSIDE_RELEASE_PERIOD
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import java.time.ZonedDateTime

// Which library entries a run updates: the category / SY group selection, then the auto-update
// restrictions. [LibraryUpdateJob.addMangaToQueue] owns the result.

/** The entries selected by [categoryId], or by the SY library group when there is no category. */
internal suspend fun LibraryUpdateJob.selectMangaToUpdate(
    categoryId: Long,
    group: Int,
    groupExtra: String?,
): List<LibraryManga> {
    val libraryManga = getLibraryManga.await()
    // SY -->
    val groupLibraryUpdateType = libraryPreferences.groupLibraryUpdateType.get()
    // SY <--
    return when {
        categoryId != -1L -> libraryManga.filter { categoryId in it.categories }
        // SY -->
        updatesWholeLibrary(group, groupLibraryUpdateType) -> wholeLibrarySelection(libraryManga)
        else -> selectByGroup(libraryManga, group, groupExtra)
        // SY <--
    }
}

// SY -->
private fun updatesWholeLibrary(group: Int, groupLibraryUpdateType: GroupLibraryMode): Boolean =
    group == LibraryGroup.BY_DEFAULT ||
        groupLibraryUpdateType == GroupLibraryMode.GLOBAL ||
        (groupLibraryUpdateType == GroupLibraryMode.ALL_BUT_UNGROUPED && group == LibraryGroup.UNGROUPED)
// SY <--

// The whole library minus the include/exclude category preferences.
private fun LibraryUpdateJob.wholeLibrarySelection(libraryManga: List<LibraryManga>): List<LibraryManga> {
    val includedCategories = libraryPreferences.updateCategories.get().map { it.toLong() }.toSet()
    val excludedCategories = libraryPreferences.updateCategoriesExclude.get().map { it.toLong() }.toSet()
    return libraryManga.filter {
        val included = includedCategories.isEmpty() || it.categories.intersect(includedCategories).isNotEmpty()
        val excluded = it.categories.intersect(excludedCategories).isNotEmpty()
        included && !excluded
    }
}

// SY -->
private suspend fun LibraryUpdateJob.selectByGroup(
    libraryManga: List<LibraryManga>,
    group: Int,
    groupExtra: String?,
): List<LibraryManga> = when (group) {
    LibraryGroup.BY_TRACK_STATUS -> {
        val trackingExtra = groupExtra?.toIntOrNull() ?: -1
        val tracks = getTracks.await().groupBy { it.mangaId }
        libraryManga.filter { (manga) ->
            val status = tracks[manga.id]?.firstNotNullOfOrNull { track ->
                TrackStatus.parseTrackerStatus(trackerManager, track.trackerId, track.status)
            } ?: TrackStatus.OTHER
            status.int == trackingExtra
        }
    }
    LibraryGroup.BY_SOURCE -> {
        val sourceExtra = groupExtra?.nullIfBlank()?.toIntOrNull()
        val source = libraryManga.map { it.manga.source }
            .distinct()
            .sorted()
            .getOrNull(sourceExtra ?: -1)
        libraryManga.filter { it.manga.source == source }
    }
    LibraryGroup.BY_STATUS -> {
        val statusExtra = groupExtra?.toLongOrNull() ?: -1
        libraryManga.filter { it.manga.status == statusExtra }
    }
    else -> {
        libraryManga // LibraryGroup.UNGROUPED and unknown groups
    }
}
// SY <--

/**
 * Drops the entries the auto-update restrictions exclude, recording each with its reason in
 * [skippedUpdates], and sorts the rest by title.
 */
internal fun LibraryUpdateJob.applyUpdateRestrictions(
    listToUpdate: List<LibraryManga>,
    skippedUpdates: MutableList<Pair<Manga, String?>>,
): List<LibraryManga> {
    val restrictions = libraryPreferences.autoUpdateMangaRestrictions.get()
    val (_, fetchWindowUpperBound) = fetchInterval.getWindow(ZonedDateTime.now())
    return listToUpdate
        // SY -->
        .distinctBy { it.manga.id }
        // SY <--
        .filter {
            val skipReason = skipReason(it, restrictions, fetchWindowUpperBound)
            if (skipReason != null) {
                skippedUpdates.add(it.manga to applicationContext.stringResource(skipReason))
            }
            skipReason == null
        }
        .sortedBy { it.manga.title }
}

private fun skipReason(
    it: LibraryManga,
    restrictions: Set<String>,
    fetchWindowUpperBound: Long,
): dev.icerock.moko.resources.StringResource? = when {
    it.manga.updateStrategy == UpdateStrategy.ONLY_FETCH_ONCE && it.totalChapters > 0L ->
        MR.strings.skipped_reason_not_always_update
    MANGA_NON_COMPLETED in restrictions && it.manga.status.toInt() == SManga.COMPLETED ->
        MR.strings.skipped_reason_completed
    MANGA_HAS_UNREAD in restrictions && it.unreadCount != 0L -> MR.strings.skipped_reason_not_caught_up
    MANGA_NON_READ in restrictions && it.totalChapters > 0L && !it.hasStarted -> MR.strings.skipped_reason_not_started
    MANGA_OUTSIDE_RELEASE_PERIOD in restrictions && it.manga.nextUpdate > fetchWindowUpperBound ->
        MR.strings.skipped_reason_not_in_release_period
    else -> null
}
