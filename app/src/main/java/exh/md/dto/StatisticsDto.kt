package exh.md.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class StatisticsDto(
    val statistics: Map<String, StatisticsMangaDto>,
)

@Serializable
internal data class StatisticsMangaDto(
    val rating: StatisticsMangaRatingDto,
)

@Serializable
internal data class StatisticsMangaRatingDto(
    val average: Double?,
    val bayesian: Double?,
)
