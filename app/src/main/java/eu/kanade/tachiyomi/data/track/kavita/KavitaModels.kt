package eu.kanade.tachiyomi.data.track.kavita

import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.model.TrackSearch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SeriesDto(
    val id: Int,
    val name: String,
    val originalName: String = "",
    @SerialName("thumbnail_url") val thumbnailUrl: String? = "",
    val localizedName: String? = "",
    val sortName: String? = "",
    val pages: Int,
    val coverImageLocked: Boolean = true,
    val pagesRead: Int,
    val userRating: Int? = 0,
    val userReview: String? = "",
    val format: Int,
    val created: String? = "",
    val libraryId: Int,
    val libraryName: String? = "",
) {
    fun toTrack(): TrackSearch = TrackSearch.create(TrackerManager.KAVITA).also {
        it.title = name
        it.summary = ""
    }
}

@Serializable
internal data class VolumeDto(
    val id: Int,
    val number: Int,
    val name: String,
    val pages: Int,
    val pagesRead: Int,
    val lastModified: String,
    val created: String,
    val seriesId: Int,
    val chapters: List<ChapterDto> = emptyList(),
)

@Serializable
internal data class ChapterDto(
    val id: Int? = -1,
    val range: String? = "",
    val number: String? = "-1",
    val pages: Int? = 0,
    val isSpecial: Boolean? = false,
    val title: String? = "",
    val pagesRead: Int? = 0,
    val coverImageLocked: Boolean? = false,
    val volumeId: Int? = -1,
    val created: String? = "",
)

@Serializable
internal data class AuthenticationDto(
    val username: String,
    val token: String,
    val apiKey: String,
)

internal class OAuth(
    val authentications: List<SourceAuth> = listOf(
        SourceAuth(sourceId = 1),
        SourceAuth(sourceId = 2),
        SourceAuth(sourceId = 3),
    ),
) {
    fun getToken(apiUrl: String): String? {
        for (authentication in authentications) {
            if (authentication.apiUrl == apiUrl) {
                return authentication.jwtToken
            }
        }
        return null
    }
}

internal data class SourceAuth(
    val sourceId: Int,
    val apiUrl: String = "",
    val jwtToken: String = "",
)
