package exh.md.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class AtHomeDto(
    val baseUrl: String,
    val chapter: AtHomeChapterDto,
)

@Serializable
internal data class AtHomeChapterDto(
    val hash: String,
    val data: List<String>,
    val dataSaver: List<String>,
)

@Serializable
internal data class AtHomeImageReportDto(
    val url: String,
    val success: Boolean,
    val bytes: Int? = null,
    val cached: Boolean? = null,
    val duration: Long,
)
