package eu.kanade.tachiyomi.data.track.hikka.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class HKMangaPagination(
    val pagination: HKPagination,
    val list: List<HKManga>,
)
