package eu.kanade.tachiyomi.data.track.shikimori.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class SMMetadata(
    val data: SMMetadataData,
)

@Serializable
internal data class SMMetadataData(
    val mangas: List<SMMetadataResult>,
)

@Serializable
internal data class SMMetadataResult(
    val id: String,
    val name: String,
    val description: String,
    val poster: SMMangaPoster,
    val personRoles: List<SMPersonRole>,
)

@Serializable
internal data class SMMangaPoster(
    val originalUrl: String,
)
