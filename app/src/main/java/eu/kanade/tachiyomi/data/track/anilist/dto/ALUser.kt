package eu.kanade.tachiyomi.data.track.anilist.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ALCurrentUserResult(
    val data: ALUserViewer,
)

@Serializable
internal data class ALUserViewer(
    @SerialName("Viewer")
    val viewer: ALUserViewerData,
)

@Serializable
internal data class ALUserViewerData(
    val id: Int,
    val name: String,
    val mediaListOptions: ALUserListOptions,
)

@Serializable
internal data class ALUserListOptions(
    val scoreFormat: String,
)
