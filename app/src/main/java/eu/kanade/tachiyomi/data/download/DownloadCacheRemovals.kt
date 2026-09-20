package eu.kanade.tachiyomi.data.download

import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.Source
import kotlinx.coroutines.sync.withLock
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga
import uy.kohesive.injekt.api.get

/**
 * Removes a chapter that has been deleted from this cache.
 *
 * @param chapter the chapter to remove.
 * @param manga the manga of the chapter.
 */
internal suspend fun DownloadCache.removeChapter(chapter: Chapter, manga: Manga) {
    rootDownloadsDirMutex.withLock {
        val sourceDir = rootDownloadsDir.sourceDirs[manga.source] ?: return
        val mangaDir = sourceDir.mangaDirs[
            provider.getMangaDirName(
                /* SY --> */ manga.ogTitle, /* SY <-- */
            ),
        ] ?: return
        provider.getValidChapterDirNames(chapter.name, chapter.scanlator, chapter.url).forEach {
            if (it in mangaDir) {
                mangaDir.chapterDirs -= it
            }
        }
    }

    notifyChanges()
}

// SY -->
internal suspend fun DownloadCache.removeFolders(folders: List<String>, manga: Manga) {
    rootDownloadsDirMutex.withLock {
        val sourceDir = rootDownloadsDir.sourceDirs[manga.source] ?: return
        val mangaDir = sourceDir.mangaDirs[provider.getMangaDirName(manga.ogTitle)] ?: return
        folders.forEach { chapter ->
            if (chapter in mangaDir) {
                mangaDir.chapterDirs -= chapter
            }
        }
    }
}

/**
 * Removes a list of chapters that have been deleted from this cache.
 *
 * @param chapters the list of chapter to remove.
 * @param manga the manga of the chapter.
 */
internal suspend fun DownloadCache.removeChapters(chapters: List<Chapter>, manga: Manga) {
    rootDownloadsDirMutex.withLock {
        val sourceDir = rootDownloadsDir.sourceDirs[manga.source] ?: return
        val mangaDir = sourceDir.mangaDirs[
            provider.getMangaDirName(
                /* SY --> */ manga.ogTitle, /* SY <-- */
            ),
        ] ?: return
        chapters.forEach { chapter ->
            provider.getValidChapterDirNames(chapter.name, chapter.scanlator, chapter.url).forEach {
                if (it in mangaDir) {
                    mangaDir.chapterDirs -= it
                }
            }
        }
    }

    notifyChanges()
}

/**
 * Removes a manga that has been deleted from this cache.
 *
 * @param manga the manga to remove.
 */
internal suspend fun DownloadCache.removeManga(manga: Manga) {
    rootDownloadsDirMutex.withLock {
        val sourceDir = rootDownloadsDir.sourceDirs[manga.source] ?: return
        val mangaDirName = provider.getMangaDirName(/* SY --> */ manga.ogTitle /* SY <-- */)
        if (sourceDir.mangaDirs.containsKey(mangaDirName)) {
            sourceDir.mangaDirs -= mangaDirName
        }
    }

    notifyChanges()
}

/**
 * Renames a manga in this cache.
 *
 * @param manga the manga being renamed.
 * @param mangaUniFile the manga's new directory.
 * @param newTitle the manga's new title.
 */
internal suspend fun DownloadCache.renameManga(manga: Manga, mangaUniFile: UniFile, newTitle: String) {
    rootDownloadsDirMutex.withLock {
        val sourceDir = rootDownloadsDir.sourceDirs[manga.source] ?: return
        val oldMangaDirName = provider.getMangaDirName(/* SY --> */ manga.ogTitle /* SY <-- */)
        var oldChapterDirs: MutableSet<String>? = null
        // Save the old name's cached chapter dirs
        if (sourceDir.mangaDirs.containsKey(oldMangaDirName)) {
            oldChapterDirs = sourceDir.mangaDirs[oldMangaDirName]?.chapterDirs
            sourceDir.mangaDirs -= oldMangaDirName
        }

        // Retrieve/create the cached manga directory for new name
        val newMangaDirName = provider.getMangaDirName(newTitle)
        var mangaDir = sourceDir.mangaDirs[newMangaDirName]
        if (mangaDir == null) {
            mangaDir = MangaDirectory(mangaUniFile)
            sourceDir.mangaDirs += newMangaDirName to mangaDir
        }

        // Add the old chapters to new name's cache
        if (!oldChapterDirs.isNullOrEmpty()) {
            mangaDir.chapterDirs += oldChapterDirs
        }
    }

    notifyChanges()
}

internal suspend fun DownloadCache.removeSource(source: Source) {
    rootDownloadsDirMutex.withLock {
        rootDownloadsDir.sourceDirs -= source.id
    }

    notifyChanges()
}
