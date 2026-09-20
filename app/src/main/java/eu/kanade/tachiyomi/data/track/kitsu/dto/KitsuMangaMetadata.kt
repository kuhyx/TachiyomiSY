package eu.kanade.tachiyomi.data.track.kitsu.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class KitsuMangaMetadata(
    val data: KitsuMangaMetadataData,
)

@Serializable
internal data class KitsuMangaMetadataData(
    val findLibraryEntryById: KitsuMangaMetadataById,
)

@Serializable
internal data class KitsuMangaMetadataById(
    val media: KitsuMangaMetadataMedia,
)

@Serializable
internal data class KitsuMangaMetadataMedia(
    val id: String,
    val titles: KitsuMangaTitle,
    val posterImage: KitsuMangaCover,
    val description: KitsuMangaDescription,
    val staff: KitsuMangaStaff,
)

@Serializable
internal data class KitsuMangaTitle(
    val preferred: String,
)

@Serializable
internal data class KitsuMangaCover(
    val original: KitsuMangaCoverUrl,
)

@Serializable
internal data class KitsuMangaCoverUrl(
    val url: String,
)

@Serializable
internal data class KitsuMangaDescription(
    val en: String?,
)

@Serializable
internal data class KitsuMangaStaff(
    val nodes: List<KitsuMangaStaffNode>,
)

@Serializable
internal data class KitsuMangaStaffNode(
    val role: String,
    val person: KitsuMangaStaffPerson,
)

@Serializable
internal data class KitsuMangaStaffPerson(
    val name: String,
)
