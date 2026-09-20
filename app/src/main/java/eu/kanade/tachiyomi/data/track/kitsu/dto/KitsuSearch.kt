package eu.kanade.tachiyomi.data.track.kitsu.dto

import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.kitsu.KitsuApi
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Serializable
internal data class KitsuSearchResult(
    val media: KitsuSearchResultData,
)

@Serializable
internal data class KitsuSearchResultData(
    val key: String,
)

@Serializable
internal data class KitsuAlgoliaSearchResult(
    val hits: List<KitsuAlgoliaSearchItem>,
)

@Serializable
internal data class KitsuAlgoliaSearchItem(
    val id: Long,
    val canonicalTitle: String,
    val chapterCount: Long?,
    val subtype: String?,
    val posterImage: KitsuSearchItemCover?,
    val synopsis: String?,
    val averageRating: Double?,
    val startDate: Long?,
    val endDate: Long?,
) {
    fun toTrack(): TrackSearch {
        return TrackSearch.create(TrackerManager.KITSU).apply {
            remoteId = this@KitsuAlgoliaSearchItem.id
            title = canonicalTitle
            totalChapters = chapterCount ?: 0
            coverUrl = posterImage?.original ?: ""
            summary = synopsis ?: ""
            trackingUrl = KitsuApi.mangaUrl(remoteId)
            score = averageRating ?: -1.0
            publishingStatus = if (endDate == null) "Publishing" else "Finished"
            publishingType = subtype ?: ""
            startDate = this@KitsuAlgoliaSearchItem.startDate?.let {
                val outputDf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                outputDf.format(Date(it * 1000))
            } ?: ""
        }
    }
}
