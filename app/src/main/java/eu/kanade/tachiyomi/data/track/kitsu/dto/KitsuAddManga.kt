package eu.kanade.tachiyomi.data.track.kitsu.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class KitsuAddMangaResult(
    val data: KitsuAddMangaItem,
)

@Serializable
internal data class KitsuAddMangaItem(
    val id: Long,
)
