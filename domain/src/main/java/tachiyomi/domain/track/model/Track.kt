package tachiyomi.domain.track.model

import java.io.Serializable

/**
 * A manga's link to an entry on one tracker (AniList, MAL, Kitsu, ...), with the last
 * state synced from it. One row per manga and tracker.
 *
 * @property id Row id.
 * @property mangaId Id of the local manga.
 * @property trackerId Id of the tracker service.
 * @property remoteId Id of the manga on the tracker.
 * @property libraryId Id of the user's list entry on the tracker; null for trackers without one.
 * @property title Title of the manga on the tracker.
 * @property lastChapterRead Chapter number the tracker has as read.
 * @property totalChapters Chapter count the tracker reports; 0 when unknown.
 * @property status The tracker's own reading-status code.
 * @property score Score on the tracker's own scale; 0 when unrated.
 * @property remoteUrl Web page of the entry on the tracker.
 * @property startDate Epoch millis reading started; 0 when unset.
 * @property finishDate Epoch millis reading finished; 0 when unset.
 * @property private Whether the entry is hidden from the tracker's public profile.
 */
public data class Track(
    val id: Long,
    val mangaId: Long,
    val trackerId: Long,
    val remoteId: Long,
    val libraryId: Long?,
    val title: String,
    val lastChapterRead: Double,
    val totalChapters: Long,
    val status: Long,
    val score: Double,
    val remoteUrl: String,
    val startDate: Long,
    val finishDate: Long,
    val private: Boolean,
) : Serializable {

    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}
