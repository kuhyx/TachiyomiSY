package eu.kanade.tachiyomi.data.track.mangabaka.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Title preference: primary, then official, native, then everything else.
private const val OTHER_TITLE_RANK = 3

private val TITLE_PRIORITIES = listOf("en", "ja-Latn", "ja", "ko-Latn", "ko", "zh-Latn", "zh")

@Serializable
internal data class MangaBakaItemResult(
    val data: MangaBakaItem,
)

@Serializable
internal data class MangaBakaSearchResult(
    val data: List<MangaBakaItem>,
)

@Serializable
internal data class MangaBakaItem(
    val id: Long,
    val cover: MangaBakaCover,
    val authors: List<String>?,
    val artists: List<String>?,
    val description: String?,
    val published: MangaBakaPublishData,
    val status: String,
    val type: String,
    val rating: Double?,
    val titles: List<MangaBakaItemTitle>?,
)

internal fun MangaBakaItem.chooseBestTitle(): String {
    // based on https://mangabaka.org/pages/announcements/15-titles-v2#finding-the-title-you-want
    // extended with zh-Latn and zh
    val bestTitlePerLanguage = TITLE_PRIORITIES.associateWith { lang ->
        titles?.filter { it.language == lang }
            ?.minByOrNull {
                when {
                    it.isPrimary -> 0
                    "official" in it.traits -> 1
                    "native" in it.traits -> 2
                    else -> OTHER_TITLE_RANK
                }
            }
    }

    return TITLE_PRIORITIES
        .firstNotNullOfOrNull { bestTitlePerLanguage[it]?.title }
        ?: titles?.firstOrNull()?.title
        ?: "ID: $id - Could not find name! (report on the MangaBaka Discord)"
}

@Serializable
internal data class MangaBakaCover(
    val x250: MangaBakaScaledCover,
)

@Serializable
internal data class MangaBakaScaledCover(
    val x1: String?,
)

@Serializable
internal data class MangaBakaPublishData(
    @SerialName("start_date")
    val startDate: String?,
)

@Serializable
internal data class MangaBakaItemTitle(
    val language: String,
    val traits: List<String>,
    val title: String,
    @SerialName("is_primary")
    val isPrimary: Boolean,
)
