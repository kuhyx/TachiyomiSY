package eu.kanade.domain.chapter.interactor

import eu.kanade.domain.download.interactor.DeleteDownload
import exh.source.MERGED_SOURCE_ID
import logcat.LogPriority
import tachiyomi.core.common.util.lang.withNonCancellableContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.interactor.GetMergedChaptersByMangaId
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.model.ChapterUpdate
import tachiyomi.domain.chapter.repository.ChapterRepository
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.MangaRepository

internal class SetReadStatus(
    private val downloadPreferences: DownloadPreferences,
    private val deleteDownload: DeleteDownload,
    private val mangaRepository: MangaRepository,
    private val chapterRepository: ChapterRepository,
    // SY -->
    private val getMergedChaptersByMangaId: GetMergedChaptersByMangaId,
    // SY <--
) {

    private val mapper = { chapter: Chapter, read: Boolean ->
        ChapterUpdate(
            read = read,
            lastPageRead = if (!read) 0 else null,
            id = chapter.id,
        )
    }

    suspend fun await(read: Boolean, vararg chapters: Chapter): Result = withNonCancellableContext {
        markRead(read, chapters.toList())
    }

    // Writes the read flag, then drops the downloads of everything just marked read (when configured).
    private suspend fun markRead(read: Boolean, chapters: List<Chapter>): Result {
        val chaptersToUpdate = chapters.filter { it.changesWhenMarked(read) }.ifEmpty { null }
            ?: return Result.NoChapters

        val failure = tryUpdate(chaptersToUpdate, read)
        if (failure != null) return Result.InternalError(failure)

        if (read && downloadPreferences.removeAfterMarkedAsRead.get()) {
            chaptersToUpdate
                .groupBy { it.mangaId }
                .forEach { (mangaId, chapters) ->
                    deleteDownload.awaitAll(
                        manga = mangaRepository.getMangaById(mangaId),
                        chapters = chapters.toTypedArray(),
                    )
                }
        }

        return Result.Success
    }

    private fun Chapter.changesWhenMarked(read: Boolean): Boolean =
        if (read) !this.read else this.read || lastPageRead > 0

    // Null on success, otherwise the logged cause; the caller carries on.
    private suspend fun tryUpdate(chapters: List<Chapter>, read: Boolean): Exception? = try {
        chapterRepository.updateAll(chapters.map { mapper(it, read) })
        null
    } catch (expected: Exception) {
        logcat(LogPriority.ERROR, expected)
        expected
    }

    suspend fun await(mangaId: Long, read: Boolean): Result = withNonCancellableContext {
        markRead(read, chapterRepository.getChapterByMangaId(mangaId))
    }

    // SY -->
    private suspend fun awaitMerged(mangaId: Long, read: Boolean) = withNonCancellableContext {
        markRead(read, getMergedChaptersByMangaId.await(mangaId, dedupe = false))
    }

    suspend fun await(manga: Manga, read: Boolean) = if (manga.source == MERGED_SOURCE_ID) {
        awaitMerged(manga.id, read)
    } else {
        await(manga.id, read)
    }
    // SY <--

    sealed interface Result {
        data object Success : Result
        data object NoChapters : Result
        data class InternalError(val error: Throwable) : Result
    }
}
