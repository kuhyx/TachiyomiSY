package eu.kanade.tachiyomi.data.track.anilist.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class ALSearchItem(
    val id: Long,
    val title: ALItemTitle,
    val coverImage: ItemCover,
    val description: String?,
    val format: String,
    val status: String?,
    val startDate: ALFuzzyDate,
    val chapters: Long?,
    val averageScore: Int?,
    val staff: ALStaff,
    val countryOfOrigin: String = "",
) {
    fun toALManga(): ALManga = ALManga(
        remoteId = id,
        title = title.userPreferred,
        imageUrl = coverImage.large,
        description = description,
        format = if (format != "MANGA") {
            format.replace("_", "-")
        } else {
            when (countryOfOrigin) {
                "KR" -> "Manhwa"
                "CN", "TW" -> "Manhua"
                else -> "Manga"
            }
        },
        publishingStatus = status ?: "",
        startDateFuzzy = startDate.toEpochMilli(),
        totalChapters = chapters ?: 0,
        averageScore = averageScore ?: -1,
        staff = staff,
    )
}

@Serializable
internal data class ALItemTitle(
    val userPreferred: String,
)

@Serializable
internal data class ItemCover(
    val large: String,
)

@Serializable
internal data class ALStaff(
    val edges: List<ALEdge>,
)

@Serializable
internal data class ALEdge(
    val role: String,
    val id: Int,
    val node: ALStaffNode,
)

@Serializable
internal data class ALStaffNode(
    val name: ALStaffName,
)

@Serializable
internal data class ALStaffName(
    val userPreferred: String? = null,
    val native: String? = null,
    val full: String? = null,
) {
    operator fun invoke(): String? = userPreferred ?: full ?: native
}
