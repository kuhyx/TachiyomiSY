package eu.kanade.tachiyomi.data.track.bangumi.dto

import kotlinx.serialization.Serializable

@Serializable
// Incomplete DTO with only our needed attributes
internal data class BGMUser(
    val username: String,
    val nickname: String?,
)
