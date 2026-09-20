package eu.kanade.domain.track.interactor

import android.content.Context
import eu.kanade.domain.track.model.toDbTrack
import eu.kanade.domain.track.model.toDomainTrack
import eu.kanade.domain.track.service.DelayedTrackingUpdateJob
import eu.kanade.domain.track.store.DelayedTrackingStore
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

internal class TrackChapter(
    private val getTracks: GetTracks,
    private val trackerManager: TrackerManager,
    private val insertTrack: InsertTrack,
    private val delayedTrackingStore: DelayedTrackingStore,
) {

    suspend fun await(context: Context, mangaId: Long, chapterNumber: Double, setupJobOnFailure: Boolean = true) {
        withNonCancellableContext {
            val tracks = getTracks.await(mangaId)
            if (tracks.isNotEmpty()) {
                tracks.mapNotNull { track ->
                    val service = trackerManager.get(track.trackerId)
                    // SY --> an unfollowed MangaDex entry is never updated
                    val unfollowedMdList = service is MdList && track.status == FollowStatus.UNFOLLOWED.long
                    // SY <--
                    if (service == null || !service.isLoggedIn || unfollowedMdList) {
                        return@mapNotNull null
                    }
                    if (chapterNumber <= track.lastChapterRead) {
                        return@mapNotNull null
                    }

                    async {
                        runCatching {
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
                }
                    .awaitAll()
                    .mapNotNull { it.exceptionOrNull() }
                    .forEach { logcat(LogPriority.WARN, it) }
            }
        }
    }
}
