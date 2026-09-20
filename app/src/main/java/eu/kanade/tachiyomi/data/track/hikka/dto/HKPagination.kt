package eu.kanade.tachiyomi.data.track.hikka.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class HKPagination(
    val total: Int,
    val pages: Int,
    val page: Int,
)
