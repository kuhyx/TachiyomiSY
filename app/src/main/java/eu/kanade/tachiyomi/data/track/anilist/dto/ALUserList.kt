package eu.kanade.tachiyomi.data.track.anilist.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ALUserListMangaQueryResult(
    val data: ALUserListMangaPage,
)

@Serializable
internal data class ALUserListMangaPage(
    @SerialName("Page")
    val page: ALUserListMediaList,
)

@Serializable
internal data class ALUserListMediaList(
    val mediaList: List<ALUserListItem>,
)

@Serializable
internal data class ALUserListItem(
    val id: Long,
    val status: String,
    val scoreRaw: Int,
    val progress: Int,
    val startedAt: ALFuzzyDate,
    val completedAt: ALFuzzyDate,
    val media: ALSearchItem,
    val private: Boolean,
) {
    fun toALUserManga(): ALUserManga {
        return ALUserManga(
            libraryId = this@ALUserListItem.id,
            listStatus = status,
            scoreRaw = scoreRaw,
            chaptersRead = progress,
            startDateFuzzy = startedAt.toEpochMilli(),
            completedDateFuzzy = completedAt.toEpochMilli(),
            manga = media.toALManga(),
            private = private,
        )
    }
}
