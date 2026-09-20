package exh.md.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class SimilarMangaDto(
    val id: String,
    val title: Map<String, String>,
    val contentRating: String,
    val matches: List<SimilarMangaMatchListDto>,
    val updatedAt: String,
)

@Serializable
internal data class SimilarMangaMatchListDto(
    val id: String,
    val title: Map<String, String>,
    val contentRating: String,
    val score: Double,
)

@Serializable
internal data class RelationListDto(
    val response: String,
    val data: List<RelationDto>,
)

@Serializable
internal data class RelationDto(
    val attributes: RelationAttributesDto,
    val relationships: List<RelationMangaDto>,
)

@Serializable
internal data class RelationMangaDto(
    val id: String,
)

@Serializable
internal data class RelationAttributesDto(
    val relation: String,
)
