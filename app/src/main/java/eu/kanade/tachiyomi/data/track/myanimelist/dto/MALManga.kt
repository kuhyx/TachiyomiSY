package eu.kanade.tachiyomi.data.track.myanimelist.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class MALManga(
    val id: Long,
    val title: String,
    val synopsis: String = "",
    @SerialName("num_chapters")
    val numChapters: Long,
    val mean: Double = -1.0,
    @SerialName("main_picture")
    val covers: MALMangaCovers?,
    val status: String,
    @SerialName("media_type")
    val mediaType: String,
    @SerialName("start_date")
    val startDate: String?,
    val authors: List<MALAuthorNode> = emptyList(),
)

@Serializable
internal data class MALAuthorNode(
    val node: MALAuthor,
    val role: String,
)

@Serializable
internal data class MALAuthor(
    val id: Int,
    @SerialName("first_name")
    val firstName: String,
    @SerialName("last_name")
    val lastName: String,
)

internal fun MALAuthor.getFullName(): String? = "$firstName $lastName".trim().ifBlank { null }

@Serializable
internal data class MALMangaCovers(
    val large: String = "",
    val medium: String,
)

@Serializable
internal data class MALMangaMetadata(
    val id: Long,
    val title: String,
    val synopsis: String?,
    @SerialName("main_picture")
    val covers: MALMangaCovers,
    val authors: List<MALAuthorNode>,
)
