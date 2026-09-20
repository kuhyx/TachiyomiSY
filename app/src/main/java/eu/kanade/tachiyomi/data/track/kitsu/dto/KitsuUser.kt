package eu.kanade.tachiyomi.data.track.kitsu.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class KitsuCurrentUserResult(
    val data: List<KitsuUser>,
)

@Serializable
internal data class KitsuUser(
    val id: String,
    val attributes: KitsuUserAttributes,
)

@Serializable
internal data class KitsuUserAttributes(
    val name: String,
)
