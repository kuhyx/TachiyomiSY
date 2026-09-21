package exh.eh

import exh.metadata.metadata.EHentaiSearchMetadata
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.model.ChapterUpdate
import tachiyomi.domain.history.model.History
import tachiyomi.domain.history.model.HistoryUpdate
import tachiyomi.domain.manga.model.FavoriteEntryAlternative

internal suspend fun EHentaiUpdateHelper.mergeHistoryInto(
    accepted: ChapterChain,
    chainsAsChapters: List<Chapter>,
    chainsAsHistory: List<History>,
) {
    val (newHistory, deleteHistory) = getHistory(
        getChaptersByMangaId.await(accepted.manga.id),
        chainsAsChapters,
        chainsAsHistory,
    )
    // Delete the duplicate history first
    deleteHistory.forEach {
        removeHistory.awaitById(it)
    }
    // Insert new history
    newHistory.forEach {
        upsertHistory.await(it)
    }
}

internal fun EHentaiUpdateHelper.getFavoriteEntryAlternative(
    accepted: ChapterChain,
    toDiscard: List<ChapterChain>,
): FavoriteEntryAlternative? {
    val favorite = toDiscard.find { it.manga.favorite } ?: return null

    val gid = EHentaiSearchMetadata.galleryId(accepted.manga.url)
    val token = EHentaiSearchMetadata.galleryToken(accepted.manga.url)

    return FavoriteEntryAlternative(
        otherGid = gid,
        otherToken = token,
        gid = EHentaiSearchMetadata.galleryId(favorite.manga.url),
        token = EHentaiSearchMetadata.galleryToken(favorite.manga.url),
    )
}

internal fun EHentaiUpdateHelper.getHistory(
    currentChapters: List<Chapter>,
    chainsAsChapters: List<Chapter>,
    chainsAsHistory: List<History>,
): Pair<List<HistoryUpdate>, List<Long>> {
    val history = chainsAsHistory.groupBy { history -> chainsAsChapters.find { it.id == history.chapterId }?.url }
    val newHistory = currentChapters.mapNotNull { chapter ->
        val newHistory = history[chapter.url]
            ?.maxByOrNull {
                it.readAt?.time ?: 0
            }
            ?.takeIf { it.chapterId != chapter.id && it.readAt != null }
        newHistory?.let { HistoryUpdate(chapter.id, it.readAt!!, it.readDuration) }
    }
    val currentChapterIds = currentChapters.map { it.id }
    val historyToDelete = chainsAsHistory.filterNot { it.chapterId in currentChapterIds }
        .map { it.id }
    return newHistory to historyToDelete
}

internal fun EHentaiUpdateHelper.getChapterList(
    accepted: ChapterChain,
    toDiscard: List<ChapterChain>,
    chainsAsChapters: List<Chapter>,
): Triple<List<ChapterUpdate>, List<Chapter>, Boolean> {
    var new = false
    val newLastPageRead = chainsAsChapters.maxOfOrNull { it.lastPageRead }
    val merged = toDiscard
        .flatMap { chain -> chain.chapters }
        .fold(accepted.chapters) { curChapters, chapter ->
            if (curChapters.any { it.url == chapter.url }) {
                curChapters.map { if (it.url == chapter.url) it.mergedWith(chapter, newLastPageRead) else it }
            } else {
                new = true
                curChapters + chapter.copyInto(accepted.manga.id, newLastPageRead)
            }
        }
        .sortedBy { it.dateUpload }
    val (updates, newChapters) = renumber(merged)
    return Triple(updates, newChapters, new)
}
