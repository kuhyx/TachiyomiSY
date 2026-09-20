package eu.kanade.tachiyomi.data.track.mangaupdates.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class MUStatus(
    val volume: Int? = null,
    val chapter: Int? = null,
)
