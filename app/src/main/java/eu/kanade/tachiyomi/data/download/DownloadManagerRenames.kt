package eu.kanade.tachiyomi.data.download

import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.util.storage.DiskUtil
import logcat.LogPriority
import tachiyomi.core.common.storage.extension
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga
import uy.kohesive.injekt.api.get

/**
 * Renames source download folder.
 *
 * @param oldSource the old source.
 * @param newSource the new source.
 */
internal fun DownloadManager.renameSource(oldSource: Source, newSource: Source) {
    val oldFolder = provider.findSourceDir(oldSource) ?: return
    val newName = provider.getSourceDirName(newSource)

    if (oldFolder.name == newName) return

    val capitalizationChanged = oldFolder.name.equals(newName, ignoreCase = true)
    if (capitalizationChanged) {
        val tempName = newName + Downloader.TMP_DIR_SUFFIX
        if (!oldFolder.renameTo(tempName)) {
            logcat(LogPriority.ERROR) { "Failed to rename source download folder: ${oldFolder.name}" }
            return
        }
    }

    if (!oldFolder.renameTo(newName)) {
        logcat(LogPriority.ERROR) { "Failed to rename source download folder: ${oldFolder.name}" }
    }
}

/**
 * Renames manga download folder.
 *
 * @param manga the manga
 * @param newTitle the new manga title.
 */
internal suspend fun DownloadManager.renameManga(manga: Manga, newTitle: String) {
    val source = sourceManager.getOrStub(manga.source)
    val oldFolder = provider.findMangaDir(/* SY --> */ manga.ogTitle /* SY <-- */, source) ?: return
    val newName = provider.getMangaDirName(newTitle)

    if (oldFolder.name == newName) return

    // just to be safe, don't allow downloads for this manga while renaming it
    downloader.removeFromQueue(manga)

    val capitalizationChanged = oldFolder.name.equals(newName, ignoreCase = true)
    if (capitalizationChanged) {
        val tempName = newName + Downloader.TMP_DIR_SUFFIX
        if (!oldFolder.renameTo(tempName)) {
            logcat(LogPriority.ERROR) { "Failed to rename manga download folder: ${oldFolder.name}" }
            return
        }
    }

    if (oldFolder.renameTo(newName)) {
        cache.renameManga(manga, oldFolder, newTitle)
    } else {
        logcat(LogPriority.ERROR) { "Failed to rename manga download folder: ${oldFolder.name}" }
    }
}

/**
 * Renames an already downloaded chapter.
 *
 * @param source the source of the manga.
 * @param manga the manga of the chapter.
 * @param oldChapter the existing chapter with the old name.
 * @param newChapter the target chapter with the new name.
 */
internal suspend fun DownloadManager.renameChapter(
    source: Source,
    manga: Manga,
    oldChapter: Chapter,
    newChapter: Chapter,
) {
    val oldNames = provider.getValidChapterDirNames(oldChapter.name, oldChapter.scanlator, oldChapter.url)
    val mangaDir = provider.getMangaDir(/* SY --> */ manga.ogTitle /* SY <-- */, source).getOrElse { e ->
        logcat(LogPriority.ERROR, e) { "Manga download folder doesn't exist. Skipping renaming after source sync" }
        return
    }

    // Assume there's only 1 version of the chapter name formats present
    val oldDownload = oldNames.asSequence()
        .mapNotNull { mangaDir.findFile(it) }
        .firstOrNull()
        ?: return

    var newName = provider.getChapterDirName(newChapter.name, newChapter.scanlator, newChapter.url)
    if (oldDownload.isFile && oldDownload.extension == "cbz") {
        newName += ".cbz"
    }

    if (oldDownload.name == newName) return

    if (oldDownload.renameTo(newName)) {
        cache.removeChapter(oldChapter, manga)
        cache.addChapter(newName, mangaDir, manga)
    } else {
        logcat(LogPriority.ERROR) { "Could not rename downloaded chapter: ${oldNames.joinToString()}" }
    }
}

internal fun DownloadManager.renameMangaDir(oldTitle: String, newTitle: String, source: Long) {
    val sourceDir = provider.findSourceDir(sourceManager.getOrStub(source)) ?: return
    val mangaDir = sourceDir.findFile(DiskUtil.buildValidFilename(oldTitle)) ?: return
    mangaDir.renameTo(DiskUtil.buildValidFilename(newTitle))
}
