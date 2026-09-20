package eu.kanade.tachiyomi.data.track.hikka.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class HKAuthTokenInfo(
    val reference: String,
    val created: Long,
    val client: HKClient,
    val scope: List<String>,
    val expiration: Long,
    val used: Long,
)
