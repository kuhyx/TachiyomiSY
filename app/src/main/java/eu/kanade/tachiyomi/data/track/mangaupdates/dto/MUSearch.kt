package eu.kanade.tachiyomi.data.track.mangaupdates.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class MUSearchResult(
    val results: List<MUSearchResultItem>,
)

@Serializable
internal data class MUSearchResultItem(
    val record: MURecord,
)
