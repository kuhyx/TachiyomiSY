package eu.kanade.tachiyomi.data.track.hikka.dto

import eu.kanade.tachiyomi.data.track.hikka.HikkaApi
import eu.kanade.tachiyomi.data.track.hikka.stringToNumber
import eu.kanade.tachiyomi.data.track.hikka.toTrackStatus
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val MILLIS_PER_SECOND = 1000L

@Serializable
internal data class HKRead(
    val reference: String,
    val note: String?,
    val updated: Long,
    val created: Long,
    val status: String,
    val chapters: Int,
    val volumes: Int,
    val rereads: Int,
    val score: Int,
    @SerialName("start_date")
    val startDate: Long? = null,
    @SerialName("end_date")
    val endDate: Long? = null,
    val content: HKManga? = null,
) {
    fun toTrack(trackId: Long): TrackSearch {
        return TrackSearch.create(trackId).apply {
            val mangaContent = this@HKRead.content
            if (mangaContent != null) {
                title = mangaContent.titleUa ?: mangaContent.titleEn ?: mangaContent.titleOriginal
                remoteId = stringToNumber(mangaContent.slug)
                libraryId = stringToNumber(mangaContent.slug)
                totalChapters = mangaContent.chapters?.toLong() ?: 0
                trackingUrl = "${HikkaApi.BASE_URL}/manga/${mangaContent.slug}"
            }

            lastChapterRead = this@HKRead.chapters.toDouble()
            score = this@HKRead.score.toDouble()
            status = toTrackStatus(this@HKRead.status)

            startedReadingDate = this@HKRead.startDate?.let { it * MILLIS_PER_SECOND } ?: 0L
            finishedReadingDate = endDate?.let { it * MILLIS_PER_SECOND } ?: 0L
        }
    }
}
