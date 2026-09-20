package eu.kanade.tachiyomi.data.track.mangaupdates.dto

import eu.kanade.tachiyomi.data.track.model.TrackSearch
import eu.kanade.tachiyomi.util.lang.htmlDecode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class MURecord(
    @SerialName("series_id")
    val seriesId: Long? = null,
    val title: String? = null,
    val url: String? = null,
    val description: String? = null,
    val image: MUImage? = null,
    val type: String? = null,
    val year: String? = null,
    @SerialName("bayesian_rating")
    val bayesianRating: Double? = null,
    @SerialName("rating_votes")
    val ratingVotes: Int? = null,
    @SerialName("latest_chapter")
    val latestChapter: Int? = null,
    val authors: List<MUAuthor>? = null,
)

internal fun MURecord.toTrackSearch(id: Long): TrackSearch {
    return TrackSearch.create(id).apply {
        remoteId = this@toTrackSearch.seriesId ?: 0L
        title = this@toTrackSearch.title?.htmlDecode() ?: ""
        totalChapters = 0
        coverUrl = this@toTrackSearch.image?.url?.original ?: ""
        summary = this@toTrackSearch.description?.htmlDecode() ?: ""
        trackingUrl = this@toTrackSearch.url ?: ""
        publishingStatus = ""
        publishingType = this@toTrackSearch.type.toString()
        startDate = this@toTrackSearch.year.toString()
    }
}

@Serializable
internal data class MUAuthor(
    val type: String? = null,
    val name: String? = null,
)
