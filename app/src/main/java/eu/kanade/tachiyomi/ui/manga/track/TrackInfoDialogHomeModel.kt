package eu.kanade.tachiyomi.ui.manga.track

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.domain.track.interactor.RefreshTracks
import eu.kanade.domain.track.model.toDbTrack
import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.tachiyomi.data.track.EnhancedTracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.source.online.MetadataSource
import eu.kanade.tachiyomi.util.system.toast
import exh.metadata.metadata.base.TrackerIdMetadata
import exh.metadata.metadata.base.raise
import exh.source.getMainSource
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.manga.interactor.GetFlatMetadataById
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.model.Track
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal class TrackInfoDialogHomeModel(
    private val mangaId: Long,
    private val sourceId: Long,
    private val getTracks: GetTracks = Injekt.get(),
    /* SY --> */
    private val trackerManager: TrackerManager = Injekt.get(),
    private val trackPreferences: TrackPreferences = Injekt.get(),
    /* SY <-- */
) : StateScreenModel<TrackInfoDialogHomeModel.State>(State()) {

    init {
        screenModelScope.launch {
            refreshTrackers()
        }

        screenModelScope.launch {
            getTracks.subscribe(mangaId)
                .catch { logcat(LogPriority.ERROR, it) }
                .distinctUntilChanged()
                .map { it.mapToTrackItem() }
                .collectLatest { trackItems -> mutableState.update { it.copy(trackItems = trackItems) } }
        }
    }

    fun registerEnhancedTracking(item: TrackItem) {
        item.tracker as EnhancedTracker
        screenModelScope.launchNonCancellable {
            val manga = Injekt.get<GetManga>().await(mangaId)
            if (manga != null) {
                try {
                    val matchResult = item.tracker.match(manga) ?: error("No match for ${manga.title}")
                    item.tracker.register(matchResult, mangaId)
                } catch (_: Exception) {
                    withUIContext { Injekt.get<Application>().toast(MR.strings.error_no_match) }
                }
            }
        }
    }

    // SY -->
    fun newSearch(navigator: Navigator, item: TrackItem, mangaTitle: String) {
        suspend fun work() {
            if (trackPreferences.resolveUsingSourceMetadata.get()) {
                // Check if the tracker id is contained in the metadata
                val result = getTrackerIdFromMetadata(item.tracker.id)
                if (result != null) {
                    mutableState.update { it.copy(isLoading = true) }

                    // Try to register tracking by id
                    val success = registerTrackingById(item.tracker.id, result)

                    mutableState.update { it.copy(isLoading = false) }

                    if (success) {
                        // Return on success
                        return
                    }
                }
            }

            // Open search screen
            navigator.push(
                TrackerSearchScreen(
                    mangaId = mangaId,
                    initialQuery = item.track?.title ?: mangaTitle,
                    currentUrl = item.track?.remoteUrl,
                    serviceId = item.tracker.id,
                ),
            )
        }
        screenModelScope.launchNonCancellable { work() }
    }

    suspend fun getTrackerIdFromMetadata(trackerId: Long): String? {
        try {
            val sourceManager = Injekt.get<SourceManager>()
            val getFlatMetadataById = Injekt.get<GetFlatMetadataById>()

            val metadataSource = sourceManager.get(sourceId)
                ?.getMainSource<MetadataSource<*, *>>()
                ?: return null

            return getFlatMetadataById.await(mangaId)?.run {
                raise(metadataSource.metaClass) as? TrackerIdMetadata
            }?.let { metadata ->
                when (trackerId) {
                    trackerManager.aniList.id -> metadata.anilistId
                    trackerManager.kitsu.id -> metadata.kitsuId
                    trackerManager.myAnimeList.id -> metadata.myAnimeListId
                    trackerManager.mangaUpdates.id -> metadata.mangaUpdatesId
                    else -> null
                }
            }
        } catch (expected: Throwable) {
            // Logged whatever the cause; the caller carries on.
            logcat(LogPriority.ERROR, expected) { "Failed to search manga on tracker by id" }
            return null
        }
    }

    suspend fun registerTrackingById(trackerId: Long, remoteId: String): Boolean {
        trackerManager.get(trackerId)?.let { tracker ->
            try {
                tracker.searchById(remoteId)?.let { track ->
                    tracker.register(track, mangaId)
                    return true
                }
            } catch (expected: Throwable) {
                // Logged whatever the cause; the caller carries on.
                logcat(LogPriority.ERROR, expected) { "Failed to register tracking by id" }
            }
        }
        return false
    }
    // SY <--

    private suspend fun refreshTrackers() {
        val refreshTracks = Injekt.get<RefreshTracks>()
        val context = Injekt.get<Application>()

        refreshTracks.await(mangaId)
            .filter { it.first != null }
            .forEach { (track, e) ->
                logcat(LogPriority.ERROR, e) {
                    "Failed to refresh track data mangaId=$mangaId for service ${track!!.id}"
                }
                withUIContext {
                    context.toast(
                        context.stringResource(
                            MR.strings.track_error,
                            track!!.name,
                            e.message ?: "",
                        ),
                    )
                }
            }
    }

    fun togglePrivate(item: TrackItem) {
        screenModelScope.launchNonCancellable {
            item.tracker.setRemotePrivate(item.track!!.toDbTrack(), !item.track.private)
        }
    }

    private fun List<Track>.mapToTrackItem(): List<TrackItem> {
        val loggedInTrackers = Injekt.get<TrackerManager>().loggedInTrackers()
        val source = Injekt.get<SourceManager>().getOrStub(sourceId)
        return loggedInTrackers
            // Map to TrackItem
            .map { service -> TrackItem(find { it.trackerId == service.id }, service) }
            // Show only if the service supports this manga's source
            .filter { (it.tracker as? EnhancedTracker)?.accept(source) ?: true }
    }

    @Immutable
    data class State(
        val trackItems: List<TrackItem> = emptyList(),
        // SY -->
        val isLoading: Boolean = false,
        // SY <--
    )
}
