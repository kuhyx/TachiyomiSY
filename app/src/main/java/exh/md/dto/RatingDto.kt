package exh.md.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class RatingResponseDto(
    val ratings: JsonElement,
)

@Serializable
internal data class PersonalRatingDto(
    val rating: Int,
    val createdAt: String,
)

@Serializable
internal data class RatingDto(val rating: Int)
