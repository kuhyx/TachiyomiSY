package eu.kanade.tachiyomi.data.track.anilist.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ALMangaMetadata(
    val data: ALMangaMetadataData,
)

@Serializable
internal data class ALMangaMetadataData(
    @SerialName("Media")
    val media: ALMangaMetadataMedia,
)

@Serializable
internal data class ALMangaMetadataMedia(
    val id: Long,
    val title: ALStaffName,
    val coverImage: ItemCover,
    val description: String?,
    val staff: ALStaff,
)
