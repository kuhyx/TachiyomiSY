package eu.kanade.tachiyomi.ui.library

import eu.kanade.tachiyomi.ui.library.LibraryScreenModel.ItemPreferences
import exh.source.MERGED_SOURCE_ID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import tachiyomi.core.common.preference.TriState
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.source.local.isLocal
import uy.kohesive.injekt.api.get

internal fun LibraryScreenModel.getLibraryItemPreferencesFlow(): Flow<ItemPreferences> {
    return combine(
        libraryPreferences.downloadBadge.changes(),
        libraryPreferences.unreadBadge.changes(),
        libraryPreferences.localBadge.changes(),
        libraryPreferences.languageBadge.changes(),
        libraryPreferences.autoUpdateMangaRestrictions.changes(),

        preferences.downloadedOnly.changes(),
        libraryPreferences.filterDownloaded.changes(),
        libraryPreferences.filterUnread.changes(),
        libraryPreferences.filterStarted.changes(),
        libraryPreferences.filterBookmarked.changes(),
        libraryPreferences.filterCompleted.changes(),
        libraryPreferences.filterIntervalCustom.changes(),
        // SY -->
        libraryPreferences.filterLewd.changes(),
        // SY <--
    ) {
        ItemPreferences(
            downloadBadge = it[0] as Boolean,
            unreadBadge = it[1] as Boolean,
            localBadge = it[2] as Boolean,
            languageBadge = it[3] as Boolean,
            skipOutsideReleasePeriod = LibraryPreferences.MANGA_OUTSIDE_RELEASE_PERIOD in it[4] as Set<*>,
            globalFilterDownloaded = it[5] as Boolean,
            filterDownloaded = it[6] as TriState,
            filterUnread = it[7] as TriState,
            filterStarted = it[8] as TriState,
            filterBookmarked = it[9] as TriState,
            filterCompleted = it[10] as TriState,
            filterIntervalCustom = it[11] as TriState,
            // SY -->
            filterLewd = it[12] as TriState,
            // SY <--
        )
    }
}

internal fun LibraryScreenModel.getFavoritesFlow(): Flow<List<LibraryItem>> {
    return combine(
        getLibraryManga.subscribe(),
        getLibraryItemPreferencesFlow(),
        downloadCache.changes,
    ) { libraryManga, preferences, _ ->
        libraryManga.map { manga -> toLibraryItem(manga, preferences) }
    }
}

internal suspend fun LibraryScreenModel.toLibraryItem(manga: LibraryManga, preferences: ItemPreferences): LibraryItem {
    // SY -->
    val downloadCount = if (manga.manga.source == MERGED_SOURCE_ID) {
        getMergedMangaById.await(manga.manga.id).sumOf { downloadManager.getDownloadCount(it) }
    } else {
        downloadManager.getDownloadCount(manga.manga)
    }
    // SY <--
    return LibraryItem(
        libraryManga = manga,
        // SY -->
        downloadCount = downloadCount,
        // SY <--
        unreadCount = manga.unreadCount,
        isLocal = manga.manga.isLocal(),
        badges = LibraryItem.Badges(
            // Each badge shows its value only when the preference asks for it.
            downloadCount = if (preferences.downloadBadge) /* SY --> */ downloadCount /* SY <-- */ else 0,
            unreadCount = if (preferences.unreadBadge) manga.unreadCount else 0,
            isLocal = preferences.localBadge && manga.manga.isLocal(),
            sourceLanguage = if (preferences.languageBadge) {
                sourceManager.getOrStub(manga.manga.source).lang
            } else {
                ""
            },
        ),
    )
}

// Flow of tracking filter preferences.
// @return map of track id with the filter value
internal fun LibraryScreenModel.getTrackingFiltersFlow(): Flow<Map<Long, TriState>> {
    return trackerManager.loggedInTrackersFlow().flatMapLatest { loggedInTrackers ->
        if (loggedInTrackers.isEmpty()) {
            flowOf(emptyMap())
        } else {
            val filterFlows = loggedInTrackers.map { tracker ->
                libraryPreferences.filterTracking(tracker.id.toInt()).changes().map { tracker.id to it }
            }
            combine(filterFlows) { it.toMap() }
        }
    }
}
