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
        val chaptersToUpdate = chapters.filter {
            when (read) {
                true -> !it.read
                false -> it.read || it.lastPageRead > 0
            }
        }
        if (chaptersToUpdate.isEmpty()) {
            return Result.NoChapters
        }

        try {
            chapterRepository.updateAll(
                chaptersToUpdate.map { mapper(it, read) },
            )
        } catch (expected: Exception) {
            // Logged whatever the cause; the caller carries on.
            logcat(LogPriority.ERROR, expected)
            return Result.InternalError(expected)
        }

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

    suspend fun await(mangaId: Long, read: Boolean): Result = withNonCancellableContext {
        await(
            read = read,
            chapters = chapterRepository
                .getChapterByMangaId(mangaId)
                .toTypedArray(),
        )
    }

    // SY -->
    private suspend fun awaitMerged(mangaId: Long, read: Boolean) = withNonCancellableContext {
        await(
            read = read,
            chapters = getMergedChaptersByMangaId
                .await(mangaId, dedupe = false)
                .toTypedArray(),
        )
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
