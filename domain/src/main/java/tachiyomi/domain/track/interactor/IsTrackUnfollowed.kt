package tachiyomi.domain.track.interactor

import tachiyomi.domain.track.model.Track

/** Whether a track is an MDList entry the user has unfollowed, which the tracker UI hides. */
public class IsTrackUnfollowed {

    /** True for an MDList track with the "unfollowed" status. */
    public fun await(track: Track): Boolean = track.trackerId == MDLIST_TRACKER_ID && track.status == UNFOLLOWED_STATUS

    private companion object {
        // TrackManager.MDLIST
        const val MDLIST_TRACKER_ID = 60L

        // FollowStatus.UNFOLLOWED
        const val UNFOLLOWED_STATUS = 0L
    }
}
