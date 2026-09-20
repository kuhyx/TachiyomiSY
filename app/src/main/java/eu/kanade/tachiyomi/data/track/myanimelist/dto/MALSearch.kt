package eu.kanade.tachiyomi.data.track.myanimelist.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class MALSearchResult(
    val data: List<MALSearchResultNode>,
    val paging: MALSearchPaging,
)

@Serializable
internal data class MALSearchResultNode(
    val node: MALManga,
)

@Serializable
internal data class MALSearchPaging(
    val next: String?,
)
