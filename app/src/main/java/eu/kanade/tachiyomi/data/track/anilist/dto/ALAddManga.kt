package eu.kanade.tachiyomi.data.track.anilist.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ALAddMangaResult(
    val data: ALAddMangaData,
)

@Serializable
internal data class ALAddMangaData(
    @SerialName("SaveMediaListEntry")
    val entry: ALAddMangaEntry,
)

@Serializable
internal data class ALAddMangaEntry(
    val id: Long,
)
