package exh.eh

import android.content.Context
import eu.kanade.domain.manga.interactor.UpdateManga
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.interactor.InsertFavoriteEntryAlternative
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
    internal val getChaptersByMangaId: GetChaptersByMangaId by injectLazy()
    private val getManga: GetManga by injectLazy()
    private val updateManga: UpdateManga by injectLazy()
    private val setMangaCategories: SetMangaCategories by injectLazy()
    private val getCategories: GetCategories by injectLazy()
    private val chapterRepository: ChapterRepository by injectLazy()
    internal val upsertHistory: UpsertHistory by injectLazy()
    internal val removeHistory: RemoveHistory by injectLazy()
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

    internal fun renumber(chapters: List<Chapter>): Pair<List<ChapterUpdate>, List<Chapter>> {
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
