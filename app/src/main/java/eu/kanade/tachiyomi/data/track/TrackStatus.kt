package eu.kanade.tachiyomi.data.track

import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.data.track.anilist.Anilist
import eu.kanade.tachiyomi.data.track.bangumi.Bangumi
import eu.kanade.tachiyomi.data.track.kitsu.Kitsu
import eu.kanade.tachiyomi.data.track.komga.Komga
import eu.kanade.tachiyomi.data.track.mangaupdates.MangaUpdates
import eu.kanade.tachiyomi.data.track.myanimelist.MyAnimeList
import eu.kanade.tachiyomi.data.track.shikimori.Shikimori
import exh.md.utils.FollowStatus
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR

internal enum class TrackStatus(val int: Int, val res: StringResource) {
    READING(int = 1, MR.strings.reading),
    REPEATING(int = 2, MR.strings.repeating),
    PLAN_TO_READ(int = 3, MR.strings.plan_to_read),
    PAUSED(int = 4, MR.strings.on_hold),
    COMPLETED(int = 5, MR.strings.completed),
    DROPPED(int = 6, MR.strings.dropped),
    OTHER(int = 7, SYMR.strings.not_tracked),
    ;

    companion object {
        // One table per tracker, keyed by that tracker's own status constant. A status a tracker has
        // no reading-state equivalent for (MangaDex UNFOLLOWED, Komga UNREAD) is simply absent.
        internal val mdListStatuses = mapOf(
            FollowStatus.READING.long to READING,
            FollowStatus.COMPLETED.long to COMPLETED,
            FollowStatus.ON_HOLD.long to PAUSED,
            FollowStatus.PLAN_TO_READ.long to PLAN_TO_READ,
            FollowStatus.DROPPED.long to DROPPED,
            FollowStatus.RE_READING.long to REPEATING,
        )
        private val myAnimeListStatuses = mapOf(
            MyAnimeList.READING to READING,
            MyAnimeList.COMPLETED to COMPLETED,
            MyAnimeList.ON_HOLD to PAUSED,
            MyAnimeList.PLAN_TO_READ to PLAN_TO_READ,
            MyAnimeList.DROPPED to DROPPED,
            MyAnimeList.REREADING to REPEATING,
        )
        private val aniListStatuses = mapOf(
            Anilist.READING to READING,
            Anilist.COMPLETED to COMPLETED,
            Anilist.ON_HOLD to PAUSED,
            Anilist.PLAN_TO_READ to PLAN_TO_READ,
            Anilist.DROPPED to DROPPED,
            Anilist.REREADING to REPEATING,
        )
        private val kitsuStatuses = mapOf(
            Kitsu.READING to READING,
            Kitsu.COMPLETED to COMPLETED,
            Kitsu.ON_HOLD to PAUSED,
            Kitsu.PLAN_TO_READ to PLAN_TO_READ,
            Kitsu.DROPPED to DROPPED,
        )
        private val shikimoriStatuses = mapOf(
            Shikimori.READING to READING,
            Shikimori.COMPLETED to COMPLETED,
            Shikimori.ON_HOLD to PAUSED,
            Shikimori.PLAN_TO_READ to PLAN_TO_READ,
            Shikimori.DROPPED to DROPPED,
            Shikimori.REREADING to REPEATING,
        )
        private val bangumiStatuses = mapOf(
            Bangumi.READING to READING,
            Bangumi.COMPLETED to COMPLETED,
            Bangumi.ON_HOLD to PAUSED,
            Bangumi.PLAN_TO_READ to PLAN_TO_READ,
            Bangumi.DROPPED to DROPPED,
        )
        private val komgaStatuses = mapOf(
            Komga.READING to READING,
            Komga.COMPLETED to COMPLETED,
        )
        private val mangaUpdatesStatuses = mapOf(
            MangaUpdates.READING_LIST to READING,
            MangaUpdates.COMPLETE_LIST to COMPLETED,
            MangaUpdates.ON_HOLD_LIST to PAUSED,
            MangaUpdates.WISH_LIST to PLAN_TO_READ,
            MangaUpdates.UNFINISHED_LIST to DROPPED,
        )

        /** The status tables by tracker id; an unknown tracker has none. */
        internal fun statusTables(trackerManager: TrackerManager): Map<Long, Map<Long, TrackStatus>> = mapOf(
            trackerManager.mdList.id to mdListStatuses,
            trackerManager.myAnimeList.id to myAnimeListStatuses,
            trackerManager.aniList.id to aniListStatuses,
            trackerManager.kitsu.id to kitsuStatuses,
            trackerManager.shikimori.id to shikimoriStatuses,
            trackerManager.bangumi.id to bangumiStatuses,
            trackerManager.komga.id to komgaStatuses,
            trackerManager.mangaUpdates.id to mangaUpdatesStatuses,
        )

        fun parseTrackerStatus(trackerManager: TrackerManager, tracker: Long, status: Long): TrackStatus? =
            statusTables(trackerManager)[tracker]?.get(status)
    }
}
