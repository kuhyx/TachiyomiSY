package eu.kanade.tachiyomi.ui.manga

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.track.model.toDomainTrack
import eu.kanade.tachiyomi.data.track.EnhancedTracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import exh.md.utils.FollowStatus
import exh.source.mangaDexSourceIds
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import logcat.LogPriority
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.interactor.InsertTrack
import tachiyomi.domain.track.model.Track
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * The track sheet's numbers on the manga screen: how many logged-in trackers apply to this
 * entry and whether any exists, kept current from the tracker table. Composed by
 * [MangaScreenModel], whose state it updates.
 */
internal class MangaTracking(
    private val model: MangaScreenModel,
    private val lifecycle: Lifecycle,
    private val mangaId: Long,
    private val trackerManager: TrackerManager = Injekt.get(),
    private val getTracks: GetTracks = Injekt.get(),
    private val insertTrack: InsertTrack = Injekt.get(),
) {
    fun observe() {
        val state = model.successState
        val manga = state?.manga ?: return

        model.screenModelScope.launchIO {
            combine(
                getTracks.subscribe(manga.id)
                    // SY -->
                    .map { trackItems ->
                        if (manga.source in mangaDexSourceIds ||
                            state.mergedData?.manga?.values.orEmpty().any {
                                it.source in mangaDexSourceIds
                            }
                        ) {
                            val mdTrack = trackItems.firstOrNull { it.trackerId == TrackerManager.MDLIST }
                            if (trackerManager.mdList.isLoggedIn && mdTrack == null) {
                                trackItems + createMdListTrack()
                            } else {
                                trackItems
                            }
                        } else {
                            trackItems
                        }
                    }
                    // SY <--
                    .catch { logcat(LogPriority.ERROR, it) },
                trackerManager.loggedInTrackersFlow(),
            ) { mangaTracks, loggedInTrackers ->
                // Show only if the service supports this manga's model.source
                val supportedTrackers = loggedInTrackers.filter {
                    (it as? EnhancedTracker)?.accept(model.source!!) ?: true
                }
                val supportedTrackerIds = supportedTrackers.map { it.id }.toHashSet()
                val supportedTrackerTracks = mangaTracks.filter { it.trackerId in supportedTrackerIds }
                // SY -->
                val trackingCount = supportedTrackerTracks.count {
                    (it.trackerId == TrackerManager.MDLIST && it.status != FollowStatus.UNFOLLOWED.long) ||
                        it.trackerId != TrackerManager.MDLIST
                }
                trackingCount to supportedTrackers.isNotEmpty()
                // SY <--
            }
                .flowWithLifecycle(lifecycle)
                .distinctUntilChanged()
                .collectLatest { (trackingCount, hasLoggedInTrackers) ->
                    model.updateSuccessState {
                        it.copy(
                            trackingCount = trackingCount,
                            hasLoggedInTrackers = hasLoggedInTrackers,
                        )
                    }
                }
        }
    }

    // SY -->
    private suspend fun createMdListTrack(): Track {
        val state = model.successState!!
        val mdManga = state.manga.takeIf { it.source in mangaDexSourceIds }
            ?: state.mergedData?.manga?.values?.find { it.source in mangaDexSourceIds }
            ?: throw IllegalArgumentException("Could not create initial track")
        val track = trackerManager.mdList.createInitialTracker(state.manga, mdManga)
            .toDomainTrack(false)!!
        insertTrack.await(track)
        return getTracks.await(mangaId).first { it.trackerId == trackerManager.mdList.id }
    }
    // SY <--
}
