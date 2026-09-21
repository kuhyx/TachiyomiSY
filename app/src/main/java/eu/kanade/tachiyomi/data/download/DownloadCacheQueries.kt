package eu.kanade.tachiyomi.data.download

import com.hippo.unifile.UniFile
import kotlinx.coroutines.sync.withLock
import tachiyomi.domain.manga.model.Manga
import uy.kohesive.injekt.api.get

/**
 * Returns true if the chapter is downloaded.
 *
 * @param chapterName the name of the chapter to query.
 * @param chapterScanlator scanlator of the chapter to query
 * @param chapterUrl the url of the chapter to query
 * @param mangaTitle the title of the manga to query.
 * @param sourceId the id of the source of the chapter.
 * @param skipCache whether to skip the directory cache and check in the filesystem.
 */
internal fun DownloadCache.isChapterDownloaded(
    chapterName: String,
    chapterScanlator: String?,
    chapterUrl: String,
    mangaTitle: String,
    sourceId: Long,
    skipCache: Boolean,
): Boolean {
    if (skipCache) {
        val source = sourceManager.getOrStub(sourceId)
        return provider.findChapterDir(chapterName, chapterScanlator, chapterUrl, mangaTitle, source) != null
    }

    renewCache()

    val sourceDir = rootDownloadsDir.sourceDirs[sourceId]
    if (sourceDir != null) {
        val mangaDir = sourceDir.mangaDirs[provider.getMangaDirName(mangaTitle)]
        if (mangaDir != null) {
            return provider.getValidChapterDirNames(
                chapterName,
                chapterScanlator,
                chapterUrl,
            ).any { it in mangaDir }
        }
    }
    return false
}

/**
 * Returns the amount of downloaded chapters.
 */
internal fun DownloadCache.getTotalDownloadCount(): Int {
    renewCache()

    return rootDownloadsDir.chapterCount()
}

/**
 * Returns the amount of downloaded chapters for a manga.
 *
 * @param manga the manga to check.
 */
internal fun DownloadCache.getDownloadCount(manga: Manga): Int {
    renewCache()

    val sourceDir = rootDownloadsDir.sourceDirs[manga.source]
    if (sourceDir != null) {
        val mangaDir = sourceDir.mangaDirs[
            provider.getMangaDirName(/* SY --> */ manga.ogTitle /* SY <-- */),
        ]
        if (mangaDir != null) {
            return mangaDir.chapterDirs.size
        }
    }
    return 0
}

/**
 * Adds a chapter that has just been download to this cache.
 *
 * @param chapterDirName the downloaded chapter's directory name.
 * @param mangaUniFile the directory of the manga.
 * @param manga the manga of the chapter.
 */
internal suspend fun DownloadCache.addChapter(chapterDirName: String, mangaUniFile: UniFile, manga: Manga) {
    rootDownloadsDirMutex.withLock {
        // Retrieve the cached source directory or cache a new one
        var sourceDir = rootDownloadsDir.sourceDirs[manga.source]
        if (sourceDir == null) {
            val source = sourceManager.get(manga.source) ?: return
            val sourceUniFile = provider.findSourceDir(source) ?: return
            sourceDir = SourceDirectory(sourceUniFile)
            rootDownloadsDir.sourceDirs += manga.source to sourceDir
        }

        // Retrieve the cached manga directory or cache a new one
        val mangaDirName = provider.getMangaDirName(/* SY --> */ manga.ogTitle /* SY <-- */)
        var mangaDir = sourceDir.mangaDirs[mangaDirName]
        if (mangaDir == null) {
            mangaDir = MangaDirectory(mangaUniFile)
            sourceDir.mangaDirs += mangaDirName to mangaDir
        }

        // Save the chapter directory
        mangaDir.chapterDirs += chapterDirName
    }

    notifyChanges()
}
