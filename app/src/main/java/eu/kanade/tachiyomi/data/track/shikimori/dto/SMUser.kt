package eu.kanade.tachiyomi.data.track.shikimori.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class SMUserResult(
    val data: SMCurrentUser,
)

@Serializable
internal data class SMCurrentUser(
    val currentUser: SMUser,
)

@Serializable
internal data class SMUser(
    val id: String,
    val nickname: String,
)
