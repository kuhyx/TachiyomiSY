package eu.kanade.domain.track.interactor

import eu.kanade.domain.track.model.toDbTrack
import eu.kanade.domain.track.model.toDomainTrack
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.EnhancedTracker
import eu.kanade.tachiyomi.data.track.Tracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.util.lang.convertEpochMillisZone
import logcat.LogPriority
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.lang.withNonCancellableContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.history.interactor.GetHistory
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.track.interactor.InsertTrack
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.ZoneOffset
import tachiyomi.domain.track.model.Track as DomainTrack

internal class AddTracks(
    private val insertTrack: InsertTrack,
    private val syncChapterProgressWithTrack: SyncChapterProgressWithTrack,
    private val getChaptersByMangaId: GetChaptersByMangaId,
    private val trackerManager: TrackerManager,
) {

    // Follow-up: update all trackers based on common data (https://github.com/kuhyx/TachiyomiSY/issues/6)
    suspend fun bind(tracker: Tracker, item: Track, mangaId: Long) = withNonCancellableContext {
        withIOContext {
            val allChapters = getChaptersByMangaId.await(mangaId)
            val hasReadChapters = allChapters.any { it.read }
            tracker.bind(item, hasReadChapters)

            item.toDomainTrack(idRequired = false)
                ?.let { bindTrack(tracker, it, mangaId, allChapters, hasReadChapters) }
        }
    }

    // Stores the bound track, pushes newer local progress and the first-read date to the tracker.
    private suspend fun bindTrack(
        tracker: Tracker,
        boundTrack: DomainTrack,
        mangaId: Long,
        allChapters: List<Chapter>,
        hasReadChapters: Boolean,
    ) {
        var track = boundTrack
        insertTrack.await(track)

        // Follow-up: merge into [SyncChapterProgressWithTrack]? (https://github.com/kuhyx/TachiyomiSY/issues/7)
        // Update chapter progress if newer chapters marked read locally
        if (hasReadChapters) {
            val latestLocalReadChapterNumber = allChapters
                .sortedBy { it.chapterNumber }
                .takeWhile { it.read }
                .lastOrNull()
                ?.chapterNumber
                ?: -1.0

            if (latestLocalReadChapterNumber > track.lastChapterRead) {
                track = track.copy(
                    lastChapterRead = latestLocalReadChapterNumber,
                )
                tracker.setRemoteLastChapterRead(track.toDbTrack(), latestLocalReadChapterNumber.toInt())
            }

            if (track.startDate <= 0) {
                val firstReadChapterDate = Injekt.get<GetHistory>().await(mangaId)
                    .sortedBy { it.readAt }
                    .firstOrNull()
                    ?.readAt

                firstReadChapterDate?.let {
                    val startDate = firstReadChapterDate.time.convertEpochMillisZone(
                        ZoneOffset.systemDefault(),
                        ZoneOffset.UTC,
                    )
                    track = track.copy(
                        startDate = startDate,
                    )
                    tracker.setRemoteStartDate(track.toDbTrack(), startDate)
                }
            }
        }

        syncChapterProgressWithTrack.await(mangaId, track, tracker)
    }

    suspend fun bindEnhancedTrackers(manga: Manga, source: Source) = withNonCancellableContext {
        withIOContext {
            trackerManager.loggedInTrackers()
                .filterIsInstance<EnhancedTracker>()
                .filter { it.accept(source) }
                .forEach { service ->
                    try {
                        service.match(manga)?.let { track ->
                            track.mangaId = manga.id
                            (service as Tracker).bind(track)
                            insertTrack.await(track.toDomainTrack(idRequired = false)!!)

                            syncChapterProgressWithTrack.await(
                                manga.id,
                                track.toDomainTrack(idRequired = false)!!,
                                service,
                            )
                        }
                    } catch (expected: Exception) {
                        // Logged whatever the cause; the caller carries on.
                        logcat(
                            LogPriority.WARN,
                            expected,
                        ) { "Could not match manga: ${manga.title} with service $service" }
                    }
                }
        }
    }
}
