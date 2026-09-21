package eu.kanade.tachiyomi.data.track.anilist.dto

import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.anilist.Anilist
import eu.kanade.tachiyomi.data.track.anilist.AnilistApi
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.util.lang.htmlDecode
import java.text.SimpleDateFormat
import java.util.Locale

internal data class ALManga(
    val remoteId: Long,
    val title: String,
    val imageUrl: String,
    val description: String?,
    val format: String,
    val publishingStatus: String,
    val startDateFuzzy: Long,
    val totalChapters: Long,
    val averageScore: Int,
    val staff: ALStaff,
) {
    fun toTrack() = TrackSearch.create(TrackerManager.ANILIST).apply {
        remoteId = this@ALManga.remoteId
        title = this@ALManga.title
        totalChapters = this@ALManga.totalChapters
        coverUrl = imageUrl
        summary = description?.htmlDecode() ?: ""
        score = averageScore.toDouble()
        trackingUrl = AnilistApi.mangaUrl(remoteId)
        publishingStatus = this@ALManga.publishingStatus
        publishingType = format
        if (startDateFuzzy != 0L) {
            startDate = formatStartDate(startDateFuzzy)
        }
        val credited = staff.edges.mapNotNull { edge -> edge.node.name()?.let { it to edge.role } }
        authors += credited.filter { (_, role) -> "Story" in role }.map { it.first }
        artists += credited.filter { (_, role) -> "Art" in role }.map { it.first }
    }
}

// An unformattable date is shown as none at all.
private fun formatStartDate(startDateFuzzy: Long): String = try {
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(startDateFuzzy)
} catch (_: IllegalArgumentException) {
    ""
}

internal data class ALUserManga(
    val libraryId: Long,
    val listStatus: String,
    val scoreRaw: Int,
    val chaptersRead: Int,
    val startDateFuzzy: Long,
    val completedDateFuzzy: Long,
    val manga: ALManga,
    val private: Boolean,
) {
    fun toTrack() = Track.create(TrackerManager.ANILIST).apply {
        remoteId = manga.remoteId
        title = manga.title
        status = toTrackStatus()
        score = scoreRaw.toDouble()
        startedReadingDate = startDateFuzzy
        finishedReadingDate = completedDateFuzzy
        lastChapterRead = chaptersRead.toDouble()
        libraryId = this@ALUserManga.libraryId
        totalChapters = manga.totalChapters
        private = this@ALUserManga.private
    }

    private fun toTrackStatus() = when (listStatus) {
        "CURRENT" -> Anilist.READING
        "COMPLETED" -> Anilist.COMPLETED
        "PAUSED" -> Anilist.ON_HOLD
        "DROPPED" -> Anilist.DROPPED
        "PLANNING" -> Anilist.PLAN_TO_READ
        "REPEATING" -> Anilist.REREADING
        else -> throw IllegalArgumentException("Unknown status: $listStatus")
    }
}
