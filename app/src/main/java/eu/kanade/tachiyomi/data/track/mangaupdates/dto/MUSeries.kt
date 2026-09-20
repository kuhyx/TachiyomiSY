package eu.kanade.tachiyomi.data.track.mangaupdates.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class MUSeries(
    val id: Long? = null,
    val title: String? = null,
)
