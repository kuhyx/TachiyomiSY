@file:OptIn(DelicateCoroutinesApi::class)

package eu.kanade.tachiyomi.data.download

import eu.kanade.tachiyomi.source.Source
import exh.log.xLogE
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.flow.map
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga
import uy.kohesive.injekt.api.get

/**
 * Deletes the directories of a list of downloaded chapters.
 *
 * @param chapters the list of chapters to delete.
 * @param manga the manga of the chapters.
 * @param source the source of the chapters.
 */
internal fun DownloadManager.deleteChapters(chapters: List<Chapter>, manga: Manga, source: Source) {
    launchIO { doDeleteChapters(chapters = chapters, manga = manga, source = source) }
}

private suspend fun DownloadManager.doDeleteChapters(chapters: List<Chapter>, manga: Manga, source: Source) {
    val filteredChapters = getChaptersToDelete(chapters, manga)
    if (filteredChapters.isEmpty()) {
        return
    }

    removeFromDownloadQueue(filteredChapters)

    val (mangaDir, chapterDirs) = provider.findChapterDirs(filteredChapters, manga, source)
    chapterDirs.forEach { it.delete() }
    cache.removeChapters(filteredChapters, manga)

    // Delete manga directory if empty
    if (mangaDir?.listFiles()?.isEmpty() == true) {
        deleteManga(manga, source, removeQueued = false)
    }
}

/**
 * Deletes the directory of a downloaded manga.
 *
 * @param manga the manga to delete.
 * @param source the source of the manga.
 * @param removeQueued whether to also remove queued downloads.
 */
internal fun DownloadManager.deleteManga(manga: Manga, source: Source, removeQueued: Boolean = true) {
    launchIO {
        if (removeQueued) {
            downloader.removeFromQueue(manga)
        }
        provider.findMangaDir(/* SY --> */ manga.ogTitle /* SY <-- */, source)?.delete()
        cache.removeManga(manga)

        // Delete source directory if empty
        val sourceDir = provider.findSourceDir(source)
        if (sourceDir?.listFiles()?.isEmpty() == true) {
            sourceDir.delete()
            cache.removeSource(source)
        }
    }
}

internal fun DownloadManager.removeFromDownloadQueue(chapters: List<Chapter>) {
    val wasRunning = downloader.isRunning
    if (wasRunning) {
        downloader.pause()
    }

    downloader.removeFromQueue(chapters)

    if (wasRunning) {
        if (queueState.value.isEmpty()) {
            downloader.stop()
        } else {
            downloader.start()
        }
    }
}

/**
 * Deletes the directories of chapters that were read or have no match.
 *
 * @param allChapters the list of chapters to delete.
 * @param manga the manga of the chapters.
 * @param source the source of the chapters.
 * @param removeRead whether read chapters are deleted.
 * @param removeNonFavorite whether chapters of non-favourite manga are deleted.
 */
internal suspend fun DownloadManager.cleanupChapters(
    allChapters: List<Chapter>,
    manga: Manga,
    source: Source,
    removeRead: Boolean,
    removeNonFavorite: Boolean,
): Int {
    var cleaned = 0

    if (removeNonFavorite && !manga.favorite) {
        val mangaFolder = provider.getMangaDir(/* SY --> */ manga.ogTitle /* SY <-- */, source)
            .getOrNull()
        if (mangaFolder != null) {
            cleaned += 1 + mangaFolder.listFiles().orEmpty().size
            mangaFolder.delete()
            cache.removeManga(manga)
            return cleaned
        }
    }

    val filesWithNoChapter = provider.findUnmatchedChapterDirs(allChapters, manga, source)
    cleaned += filesWithNoChapter.size
    cache.removeFolders(filesWithNoChapter.mapNotNull { it.name }, manga)
    filesWithNoChapter.forEach { it.delete() }

    if (removeRead) {
        val readChapters = allChapters.filter { it.read }
        val readChapterDirs = provider.findChapterDirs(readChapters, manga, source)
        readChapterDirs.second.forEach { it.delete() }
        cleaned += readChapterDirs.second.size
        cache.removeChapters(readChapters, manga)
    }

    if (cache.getDownloadCount(manga) == 0) {
        val mangaFolder = provider.getMangaDir(/* SY --> */ manga.ogTitle /* SY <-- */, source).getOrNull()
        if (mangaFolder != null && !mangaFolder.listFiles().isNullOrEmpty()) {
            mangaFolder.delete()
            cache.removeManga(manga)
        } else {
            xLogE("Cache and download folder doesn't match for " + /* SY --> */ manga.ogTitle /* SY <-- */)
        }
    }
    return cleaned
}

/**
 * Adds a list of chapters to be deleted later.
 *
 * @param chapters the list of chapters to delete.
 * @param manga the manga of the chapters.
 */
internal suspend fun DownloadManager.enqueueChaptersToDelete(chapters: List<Chapter>, manga: Manga) {
    pendingDeleter.addChapters(getChaptersToDelete(chapters, manga), manga)
}

/**
 * Triggers the execution of the deletion of pending chapters.
 */
internal fun DownloadManager.deletePendingChapters() {
    val pendingChapters = pendingDeleter.getPendingChapters()
    for ((manga, chapters) in pendingChapters) {
        val source = sourceManager.get(manga.source) ?: continue
        deleteChapters(chapters, manga, source)
    }
}

internal suspend fun DownloadManager.getChaptersToDelete(chapters: List<Chapter>, manga: Manga): List<Chapter> {
    // Retrieve the categories that are set to exclude from being deleted on read
    val categoriesToExclude = downloadPreferences.removeExcludeCategories.get().map(String::toLong)

    val categoriesForManga = getCategories.await(manga.id)
        .map { it.id }
        .ifEmpty { listOf(0) }
    val filteredCategoryManga = if (categoriesForManga.intersect(categoriesToExclude).isNotEmpty()) {
        chapters.filterNot { it.read }
    } else {
        chapters
    }

    return if (!downloadPreferences.removeBookmarkedChapters.get()) {
        filteredCategoryManga.filterNot { it.bookmark }
    } else {
        filteredCategoryManga
    }
}
