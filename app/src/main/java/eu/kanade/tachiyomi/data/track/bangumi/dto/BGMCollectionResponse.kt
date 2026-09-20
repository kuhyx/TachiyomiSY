package eu.kanade.tachiyomi.data.track.bangumi.dto

import eu.kanade.tachiyomi.data.track.bangumi.Bangumi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
// Incomplete DTO with only our needed attributes
internal data class BGMCollectionResponse(
    val rate: Int?,
    val type: Int?,
    @SerialName("ep_status")
    val epStatus: Int? = 0,
    @SerialName("vol_status")
    val volStatus: Int? = 0,
    val private: Boolean = false,
    val subject: BGMSlimSubject? = null,
) {
    // Bangumi's collection types are numbered exactly like the tracker's status constants.
    fun getStatus(): Long = type?.toLong()?.takeIf { it in Bangumi.PLAN_TO_READ..Bangumi.DROPPED }
        ?: throw IllegalArgumentException("Unknown status: $type")
}

@Serializable
// Incomplete DTO with only our needed attributes
internal data class BGMSlimSubject(
    val volumes: Int?,
    val eps: Int?,
)
