package exh.eh

import android.content.Context
import eu.kanade.domain.manga.interactor.UpdateManga
import exh.metadata.metadata.EHentaiSearchMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.JsonObject
import mihon.core.common.extensions.EMPTY
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.interactor.SetMangaCategories
import tachiyomi.domain.chapter.interactor.GetChapterByUrl
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.model.ChapterUpdate
import tachiyomi.domain.chapter.repository.ChapterRepository
import tachiyomi.domain.history.interactor.GetHistory
import tachiyomi.domain.history.interactor.RemoveHistory
import tachiyomi.domain.history.interactor.UpsertHistory
import tachiyomi.domain.history.model.History
import tachiyomi.domain.history.model.HistoryUpdate
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.interactor.InsertFavoriteEntryAlternative
import tachiyomi.domain.manga.model.FavoriteEntryAlternative
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaUpdate
import uy.kohesive.injekt.injectLazy
import java.io.File

internal data class ChapterChain(val manga: Manga, val chapters: List<Chapter>, val history: List<History>)

internal class EHentaiUpdateHelper(context: Context) {
    val parentLookupTable =
        MemAutoFlushingLookupTable(
            File(context.filesDir, "exh-plt.maftable"),
            GalleryEntry.Serializer(),
        )
    private val getChapterByUrl: GetChapterByUrl by injectLazy()
    private val getChaptersByMangaId: GetChaptersByMangaId by injectLazy()
    private val getManga: GetManga by injectLazy()
    private val updateManga: UpdateManga by injectLazy()
    private val setMangaCategories: SetMangaCategories by injectLazy()
    private val getCategories: GetCategories by injectLazy()
    private val chapterRepository: ChapterRepository by injectLazy()
    private val upsertHistory: UpsertHistory by injectLazy()
    private val removeHistory: RemoveHistory by injectLazy()
    private val getHistoryByMangaId: GetHistory by injectLazy()
    private val insertFavoriteEntryAlternative: InsertFavoriteEntryAlternative by injectLazy()

    /**
     * @param sourceId the source the chapters were fetched from
     * @param chapters Cannot be an empty list!
     *
     * @return Triple<Accepted, Discarded, HasNew>
     */
    suspend fun acceptRootAndDiscardOthers(
        sourceId: Long,
        chapters: List<Chapter>,
    ): Triple<ChapterChain, List<ChapterChain>, List<Chapter>> {
        val chains = findChains(chapters).filter { it.manga.source == sourceId }
        // Accept oldest chain
        val accepted = chains.minBy { it.manga.id }
        val toDiscard = chains.filter { it.manga.favorite && it.manga.id != accepted.manga.id }
        if (toDiscard.isEmpty()) {
            /*val notNeeded = chains.filter { it.manga.id != accepted.manga.id }
            val (newChapters, new) = getChapterList(accepted, notNeeded, chainsAsChapters)
            val newAccepted = ChapterChain(accepted.manga, newChapters)

            // Insert new chapters for accepted manga
            db.insertChapters(newAccepted.chapters).await()*/
            return Triple(accepted, emptyList(), emptyList())
        }

        val chainsAsChapters = chains.flatMap { it.chapters }
        // Copy chain chapters to curChapters
        val (chapterUpdates, newChapters, _) = getChapterList(accepted, toDiscard, chainsAsChapters)
        val newAccepted = ChapterChain(accepted.manga, newChapters, emptyList())

        // Apply changes to all manga
        updateManga.awaitAll(favoriteSwap(accepted, toDiscard))
        // Insert new chapters for accepted manga
        chapterRepository.updateAll(chapterUpdates)
        chapterRepository.addAll(newChapters)
        mergeHistoryInto(accepted, chainsAsChapters, chains.flatMap { it.history })

        // Update favorites entry database
        getFavoriteEntryAlternative(accepted, toDiscard)?.let { insertFavoriteEntryAlternative.await(it) }

        // Copy categories from all chains to accepted manga
        val rootsToMutate = toDiscard + newAccepted
        val newCategories = rootsToMutate.flatMap { chapterChain ->
            getCategories.await(chapterChain.manga.id).map { it.id }
        }.distinct()
        rootsToMutate.forEach {
            setMangaCategories.await(it.manga.id, newCategories)
        }
        return Triple(newAccepted, toDiscard, newChapters)
    }

    // Every library entry that shares a chapter url with [chapters], with its chapters and history.
    private suspend fun findChains(chapters: List<Chapter>): List<ChapterChain> {
        return chapters
            .flatMap { chapter -> getChapterByUrl.await(chapter.url).map { it.mangaId } }
            .distinct()
            .mapNotNull { mangaId ->
                coroutineScope {
                    val manga = async(Dispatchers.IO) { getManga.await(mangaId) }
                    val chapterList = async(Dispatchers.IO) { getChaptersByMangaId.await(mangaId) }
                    val history = async(Dispatchers.IO) { getHistoryByMangaId.await(mangaId) }
                    manga.await()?.let { ChapterChain(it, chapterList.await(), history.await()) }
                }
            }
    }

    // Unfavourites the discarded chains and favourites the accepted one if it wasn't already.
    private fun favoriteSwap(accepted: ChapterChain, toDiscard: List<ChapterChain>): List<MangaUpdate> {
        val mangaUpdates = toDiscard.map { MangaUpdate(id = it.manga.id, favorite = false, dateAdded = 0) }
        if (accepted.manga.favorite) return mangaUpdates
        return mangaUpdates + MangaUpdate(
            id = accepted.manga.id,
            favorite = true,
            dateAdded = System.currentTimeMillis(),
        )
    }

    private suspend fun mergeHistoryInto(
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

    private fun getFavoriteEntryAlternative(
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

    private fun getHistory(
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

    private fun getChapterList(
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

    // Union of read/bookmark state; a chapter never opened inherits the furthest page read in the chain.
    private fun Chapter.mergedWith(other: Chapter, newLastPageRead: Long?): Chapter {
        var lastPageRead = lastPageRead.coerceAtLeast(other.lastPageRead)
        if (newLastPageRead != null && lastPageRead <= 0) {
            lastPageRead = newLastPageRead
        }
        return copy(read = read || other.read, lastPageRead = lastPageRead, bookmark = bookmark || other.bookmark)
    }

    // A copy of this chapter for [mangaId], unsaved (id -1) and unnumbered until [renumber].
    private fun Chapter.copyInto(mangaId: Long, newLastPageRead: Long?): Chapter = Chapter(
        id = -1,
        mangaId = mangaId,
        url = url,
        name = name,
        read = read,
        bookmark = bookmark,
        lastPageRead = if (newLastPageRead != null && lastPageRead <= 0) newLastPageRead else lastPageRead,
        dateFetch = dateFetch,
        dateUpload = dateUpload,
        chapterNumber = -1.0,
        scanlator = null,
        sourceOrder = -1,
        lastModifiedAt = 0,
        version = 0,
        memo = JsonObject.EMPTY,
    )

    // Names and numbers the chapters "v1..vN" by upload order; new ones are inserted, existing ones updated.
    private fun renumber(chapters: List<Chapter>): Pair<List<ChapterUpdate>, List<Chapter>> {
        val updates = mutableListOf<ChapterUpdate>()
        val newChapters = mutableListOf<Chapter>()
        chapters.forEachIndexed { index, chapter ->
            val name = "v${index + 1}: " + chapter.name.substringAfter(" ")
            val chapterNumber = index + 1.0
            val sourceOrder = chapters.lastIndex - index.toLong()
            if (chapter.id == -1L) {
                newChapters.add(chapter.copy(name = name, chapterNumber = chapterNumber, sourceOrder = sourceOrder))
            } else {
                updates.add(
                    ChapterUpdate(
                        id = chapter.id,
                        name = name.takeUnless { chapter.name == it },
                        chapterNumber = chapterNumber.takeUnless { chapter.chapterNumber == it },
                        sourceOrder = sourceOrder.takeUnless { chapter.sourceOrder == it },
                    ),
                )
            }
        }
        return updates.toList() to newChapters.toList()
    }
}

internal data class GalleryEntry(val gId: String, val gToken: String) {
    class Serializer : MemAutoFlushingLookupTable.EntrySerializer<GalleryEntry> {
        /**
         * Serialize an entry as a String.
         */
        override fun write(entry: GalleryEntry) = with(entry) { "$gId:$gToken" }

        /**
         * Read an entry from a String.
         */
        override fun read(string: String): GalleryEntry {
            val colonIndex = string.indexOf(':')
            return GalleryEntry(
                string.substring(0, colonIndex),
                string.substring(colonIndex + 1, string.length),
            )
        }
    }
}
