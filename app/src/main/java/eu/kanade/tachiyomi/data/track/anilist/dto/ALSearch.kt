package eu.kanade.tachiyomi.data.track.anilist.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ALSearchResult(
    val data: ALSearchPage,
)

@Serializable
internal data class ALSearchPage(
    @SerialName("Page")
    val page: ALSearchMedia,
)

@Serializable
internal data class ALSearchMedia(
    val media: List<ALSearchItem>,
)

// SY -->
@Serializable
internal data class ALIdSearchResult(
    val data: ALIdSearchMedia,
)

@Serializable
internal data class ALIdSearchMedia(
    @SerialName("Media")
    val media: ALSearchItem,
)
// SY <--
