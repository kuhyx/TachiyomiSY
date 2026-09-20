package eu.kanade.tachiyomi.data.track.hikka.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class HKUser(
    val reference: String,
    val username: String,
)
