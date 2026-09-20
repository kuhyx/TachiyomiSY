package eu.kanade.tachiyomi.data.track.bangumi.dto

import eu.kanade.tachiyomi.data.track.model.TrackSearch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class BGMSearchResult(
    val total: Int,
    val limit: Int,
    val offset: Int,
    val data: List<BGMSubject> = emptyList(),
)

@Serializable
// Incomplete DTO with only our needed attributes
internal data class BGMSubject(
    val id: Long,
    @SerialName("name_cn")
    val nameCn: String,
    val name: String,
    val summary: String?,
    val date: String?, // YYYY-MM-DD
    val images: BGMSubjectImages?,
    val volumes: Long = 0,
    val eps: Long = 0,
    val rating: BGMSubjectRating?,
    val platform: String?,
    // SY -->
    val infobox: List<Infobox> = emptyList(),
    // SY <--
) {
    fun toTrackSearch(trackId: Long): TrackSearch = TrackSearch.create(trackId).apply {
        remoteId = this@BGMSubject.id
        title = nameCn.ifBlank { name }
        coverUrl = images?.common.orEmpty()
        summary = if (nameCn.isNotBlank()) {
            "作品原名：$name" + this@BGMSubject.summary?.let { "\n${it.trim()}" }.orEmpty()
        } else {
            this@BGMSubject.summary?.trim().orEmpty()
        }
        score = rating?.score ?: -1.0
        trackingUrl = "https://bangumi.tv/subject/${this@BGMSubject.id}"
        totalChapters = eps
        startDate = date ?: ""
    }
}

@Serializable
// Incomplete DTO with only our needed attributes
internal data class BGMSubjectImages(
    val common: String?,
)

@Serializable
// Incomplete DTO with only our needed attributes
internal data class BGMSubjectRating(
    val score: Double?,
)
