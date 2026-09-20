package eu.kanade.tachiyomi.data.track.hikka.dto

import eu.kanade.tachiyomi.data.track.hikka.HikkaApi
import eu.kanade.tachiyomi.data.track.hikka.stringToNumber
import eu.kanade.tachiyomi.data.track.hikka.toTrackStatus
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Locale

private const val MILLIS_PER_SECOND = 1000L

@Serializable
internal data class HKManga(
    @SerialName("data_type")
    val dataType: String,
    @SerialName("title_original")
    val titleOriginal: String,
    @SerialName("media_type")
    val mediaType: String?,
    @SerialName("title_ua")
    val titleUa: String? = null,
    @SerialName("title_en")
    val titleEn: String? = null,
    val chapters: Int? = null,
    val volumes: Int? = null,
    @SerialName("translated_ua")
    val translatedUa: Boolean,
    val status: String,
    val image: String,
    val year: Int? = null,
    @SerialName("scored_by")
    val scoredBy: Int,
    val score: Double,
    val slug: String,
    @SerialName("start_date")
    val startDate: Long? = null,
    val read: List<HKRead>? = emptyList(),
) {
    fun toTrack(trackId: Long): TrackSearch {
        return TrackSearch.create(trackId).apply {
            remoteId = stringToNumber(this@HKManga.slug)
            title = this@HKManga.titleUa ?: this@HKManga.titleEn ?: this@HKManga.titleOriginal
            totalChapters = this@HKManga.chapters?.toLong() ?: 0
            coverUrl = this@HKManga.image
            score = this@HKManga.score
            trackingUrl = "${HikkaApi.BASE_URL}/manga/${this@HKManga.slug}"
            publishingStatus = this@HKManga.status
            publishingType = this@HKManga.mediaType?.replace("_", " ").orEmpty()

            this@HKManga.startDate?.takeIf { it != 0L }?.let {
                val outputDf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                startDate = try {
                    outputDf.format(it * MILLIS_PER_SECOND)
                } catch (_: Exception) {
                    ""
                }
            }

            val userProgress = read?.firstOrNull()
            if (userProgress != null) {
                status = toTrackStatus(userProgress.status)
                lastChapterRead = userProgress.chapters.toDouble()
                score = userProgress.score.toDouble()
                startedReadingDate = (userProgress.startDate ?: 0L) * MILLIS_PER_SECOND
                finishedReadingDate = (userProgress.endDate ?: 0L) * MILLIS_PER_SECOND
            }
        }
    }
}
