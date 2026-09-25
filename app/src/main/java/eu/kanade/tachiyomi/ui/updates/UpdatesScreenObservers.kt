package eu.kanade.tachiyomi.ui.updates

import androidx.compose.runtime.getValue
import androidx.compose.ui.util.fastFilter
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.data.download.getQueuedDownloadOrNull
import eu.kanade.tachiyomi.data.download.model.Download
import eu.kanade.tachiyomi.ui.updates.UpdatesScreenModel.ItemPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import logcat.LogPriority
import tachiyomi.core.common.preference.TriState
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.manga.model.applyFilter
import tachiyomi.domain.updates.model.UpdatesWithRelations
import uy.kohesive.injekt.api.get
import java.time.ZonedDateTime

private const val UPDATES_HISTORY_MONTHS = 3L

internal fun UpdatesScreenModel.observeUpdates() {
    // Set date limit for recent chapters
    val limit = ZonedDateTime.now().minusMonths(UPDATES_HISTORY_MONTHS).toInstant()

    // launchIn rather than launch { collect }: the download queue never completes, so nothing follows the collect.
    combine(
        // needed for SQL filters (unread, started, bookmarked, etc)
        getUpdatesItemPreferenceFlow()
            .distinctUntilChanged()
            .flatMapLatest {
                getUpdates.subscribe(
                    limit,
                    unread = it.filterUnread.toBooleanOrNull(),
                    started = it.filterStarted.toBooleanOrNull(),
                    bookmarked = it.filterBookmarked.toBooleanOrNull(),
                    hideExcludedScanlators = it.filterExcludedScanlators,
                ).distinctUntilChanged()
            },
        downloadCache.changes,
        downloadManager.queueState,
        // needed for Kotlin filters (downloaded)
        getUpdatesItemPreferenceFlow().distinctUntilChanged { old, new ->
            old.filterDownloaded == new.filterDownloaded
        },
    ) { updates, _, _, itemPreferences ->
        applyFilters(toUpdateItems(updates), itemPreferences)
    }
        .onEach { updateItems ->
            updateState {
                it.copy(
                    isLoading = false,
                    items = updateItems,
                )
            }
        }
        .flowOn(Dispatchers.IO)
        .launchIn(screenModelScope)
}

internal fun UpdatesScreenModel.observeDownloadState() {
    screenModelScope.launchIO {
        merge(downloadManager.statusFlow(), downloadManager.progressFlow())
            .catch { logcat(LogPriority.ERROR, it) }
            .collect(this@observeDownloadState::updateDownloadState)
    }
}

internal fun UpdatesScreenModel.observeActiveFilters() {
    getUpdatesItemPreferenceFlow()
        .map { prefs ->
            listOf(
                prefs.filterUnread,
                prefs.filterDownloaded,
                prefs.filterStarted,
                prefs.filterBookmarked,
            )
                .any { it != TriState.DISABLED }
        }
        .distinctUntilChanged()
        .onEach {
            updateState { state ->
                state.copy(hasActiveFilters = it)
            }
        }
        .launchIn(screenModelScope)
}

internal fun UpdatesScreenModel.applyFilters(
    items: List<UpdatesItem>,
    preferences: ItemPreferences,
): List<UpdatesItem> {
    val filterDownloaded = preferences.filterDownloaded

    val filterFnDownloaded: (UpdatesItem) -> Boolean = {
        applyFilter(filterDownloaded) {
            it.downloadStateProvider() == Download.State.DOWNLOADED
        }
    }

    return items.fastFilter {
        filterFnDownloaded(it)
    }
}

internal fun UpdatesScreenModel.toUpdateItems(updates: List<UpdatesWithRelations>): List<UpdatesItem> {
    return updates
        .map { update ->
            val activeDownload = downloadManager.getQueuedDownloadOrNull(update.chapterId)
            val downloaded = downloadManager.isChapterDownloaded(
                update.chapterName,
                update.scanlator,
                update.chapterUrl,
                // SY -->
                update.ogMangaTitle,
                // SY <--
                update.sourceId,
            )
            val downloadState = when {
                activeDownload != null -> activeDownload.status
                downloaded -> Download.State.DOWNLOADED
                else -> Download.State.NOT_DOWNLOADED
            }
            UpdatesItem(
                update = update,
                downloadStateProvider = { downloadState },
                downloadProgressProvider = { activeDownload?.progress ?: 0 },
                selected = update.chapterId in selectedChapterIds,
            )
        }
}

internal fun UpdatesScreenModel.getUpdatesItemPreferenceFlow(): Flow<ItemPreferences> {
    return combine(
        updatesPreferences.filterDownloaded.changes(),
        updatesPreferences.filterUnread.changes(),
        updatesPreferences.filterStarted.changes(),
        updatesPreferences.filterBookmarked.changes(),
        updatesPreferences.filterExcludedScanlators.changes(),
    ) { downloaded, unread, started, bookmarked, excludedScanlators ->
        ItemPreferences(
            filterDownloaded = downloaded,
            filterUnread = unread,
            filterStarted = started,
            filterBookmarked = bookmarked,
            filterExcludedScanlators = excludedScanlators,
        )
    }
}
