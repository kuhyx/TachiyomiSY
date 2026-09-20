package exh.eh

import kotlinx.serialization.Serializable

@Serializable
internal data class EHentaiUpdaterStats(
    val startTime: Long,
    val possibleUpdates: Int,
    val updateCount: Int,
)
