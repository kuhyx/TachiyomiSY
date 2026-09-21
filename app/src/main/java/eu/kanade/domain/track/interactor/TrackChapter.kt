package eu.kanade.domain.track.interactor

import android.content.Context
import eu.kanade.domain.track.model.toDbTrack
import eu.kanade.domain.track.model.toDomainTrack
import eu.kanade.domain.track.service.DelayedTrackingUpdateJob
import eu.kanade.domain.track.store.DelayedTrackingStore
import eu.kanade.tachiyomi.data.track.Tracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.mdlist.MdList
import exh.md.utils.FollowStatus
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import logcat.LogPriority
import tachiyomi.core.common.util.lang.withNonCancellableContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.interactor.InsertTrack
import tachiyomi.domain.track.model.Track

internal class TrackChapter(
    private val getTracks: GetTracks,
    private val trackerManager: TrackerManager,
    private val insertTrack: InsertTrack,
    private val delayedTrackingStore: DelayedTrackingStore,
) {

    suspend fun await(context: Context, mangaId: Long, chapterNumber: Double, setupJobOnFailure: Boolean = true) {
        withNonCancellableContext {
            getTracks.await(mangaId)
                .mapNotNull { track -> updatableTracker(track)?.let { service -> track to service } }
                .filter { (track, _) -> chapterNumber > track.lastChapterRead }
                .map { (track, service) ->
                    async { runCatching { push(context, service, track, chapterNumber, setupJobOnFailure) } }
                }
                .awaitAll()
                .mapNotNull { it.exceptionOrNull() }
                .forEach { logcat(LogPriority.WARN, it) }
        }
    }

    // The tracker behind [track], or null when it cannot take an update.
    private fun updatableTracker(track: Track): Tracker? {
        val service = trackerManager.get(track.trackerId) ?: return null
        // SY --> an unfollowed MangaDex entry is never updated
        val unfollowedMdList = service is MdList && track.status == FollowStatus.UNFOLLOWED.long
        // SY <--
        return service.takeIf { it.isLoggedIn && !unfollowedMdList }
    }

    private suspend fun push(
        context: Context,
        service: Tracker,
        track: Track,
        chapterNumber: Double,
        setupJobOnFailure: Boolean,
    ) {
        try {
            val updatedTrack = service.refresh(track.toDbTrack())
                .toDomainTrack(idRequired = true)!!
                .copy(lastChapterRead = chapterNumber)
            service.update(updatedTrack.toDbTrack(), true)
            insertTrack.await(updatedTrack)
            delayedTrackingStore.remove(track.id)
        } catch (expected: Exception) {
            // Rethrown (or wrapped) whatever the cause.
            delayedTrackingStore.add(track.id, chapterNumber)
            if (setupJobOnFailure) {
                DelayedTrackingUpdateJob.setupTask(context)
            }
            throw expected
        }
    }
}
