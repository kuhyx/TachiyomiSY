package eu.kanade.tachiyomi.data.download

import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.util.lang.Hash.md5
import eu.kanade.tachiyomi.util.storage.DiskUtil
import tachiyomi.domain.chapter.model.Chapter
import uy.kohesive.injekt.api.get

/**
 * Returns the download directory name for a source.
 *
 * @param source the source to query.
 */
internal fun DownloadProvider.getSourceDirName(source: Source): String {
    return DiskUtil.buildValidFilename(
        source.toString(),
        disallowNonAscii = libraryPreferences.disallowNonAsciiFilenames.get(),
    )
}

/**
 * Returns the download directory name for a manga.
 *
 * @param mangaTitle the title of the manga to query.
 */
internal fun DownloadProvider.getMangaDirName(mangaTitle: String): String {
    return DiskUtil.buildValidFilename(
        mangaTitle,
        disallowNonAscii = libraryPreferences.disallowNonAsciiFilenames.get(),
    )
}

/**
 * Returns the chapter directory name for a chapter.
 *
 * @param chapterName the name of the chapter to query.
 * @param chapterScanlator scanlator of the chapter to query.
 * @param chapterUrl url of the chapter to query.
 * @param disallowNonAsciiFilenames whether non-ASCII characters are stripped from the name.
 * @param includeChapterUrlHash whether a hash of the url is appended to the name.
 */
internal fun DownloadProvider.getChapterDirName(
    chapterName: String,
    chapterScanlator: String?,
    chapterUrl: String,
    disallowNonAsciiFilenames: Boolean = libraryPreferences.disallowNonAsciiFilenames.get(),
    includeChapterUrlHash: Boolean = downloadPreferences.includeChapterUrlHash.get(),
): String {
    var dirName = sanitizeChapterName(chapterName)
    if (!chapterScanlator.isNullOrBlank()) {
        dirName = chapterScanlator + "_" + dirName
    }
    dirName =
        DiskUtil.buildValidFilename(
            dirName,
            DiskUtil.MAX_FILE_NAME_BYTES - RESERVED_NAME_BYTES,
            disallowNonAsciiFilenames,
        )
    if (includeChapterUrlHash) dirName += "_" + md5(chapterUrl).take(URL_HASH_CHARS)
    return dirName
}

// Returns list of names that might have been previously used as
// the directory name for a chapter.
// Add to this list if naming pattern ever changes.
// @param chapterName the name of the chapter to query.
// @param chapterScanlator scanlator of the chapter to query.
// @param chapterUrl url of the chapter to query.
internal fun DownloadProvider.getLegacyChapterDirNames(
    chapterName: String,
    chapterScanlator: String?,
    chapterUrl: String,
): List<String> {
    val sanitizedChapterName = sanitizeChapterName(chapterName)
    val chapterNameV1 = DiskUtil.buildValidFilename(
        if (chapterScanlator.isNullOrBlank()) sanitizedChapterName else "${chapterScanlator}_$sanitizedChapterName",
    )

    // Get the filename that would be generated if the user were
    // using the other value for the disallow non-ASCII
    // filenames setting. This ensures that chapters downloaded
    // before the user changed the setting can still be found.
    val otherChapterDirName =
        getChapterDirName(
            chapterName,
            chapterScanlator,
            chapterUrl,
            !libraryPreferences.disallowNonAsciiFilenames.get(),
            !downloadPreferences.includeChapterUrlHash.get(),
        )

    return buildList(2) {
        // Chapter name without hash (unable to handle duplicate
        // chapter names)
        add(chapterNameV1)
        add(otherChapterDirName)
    }
}

// Return the new name for the chapter (in case it's empty or blank).
// @param chapterName the name of the chapter
internal fun DownloadProvider.sanitizeChapterName(chapterName: String): String {
    return chapterName.ifBlank {
        "Chapter"
    }
}

internal fun DownloadProvider.isChapterDirNameChanged(oldChapter: Chapter, newChapter: Chapter): Boolean {
    return getChapterDirName(oldChapter.name, oldChapter.scanlator, oldChapter.url) !=
        getChapterDirName(newChapter.name, newChapter.scanlator, newChapter.url)
}

/**
 * Returns valid downloaded chapter directory names.
 *
 * @param chapterName the name of the chapter.
 * @param chapterScanlator scanlator of the chapter.
 * @param chapterUrl url of the chapter.
 */
internal fun DownloadProvider.getValidChapterDirNames(
    chapterName: String,
    chapterScanlator: String?,
    chapterUrl: String,
): List<String> {
    val chapterDirName = getChapterDirName(chapterName, chapterScanlator, chapterUrl)
    val legacyChapterDirNames = getLegacyChapterDirNames(chapterName, chapterScanlator, chapterUrl)

    return buildList {
        // Folder of images
        add(chapterDirName)
        // Archived chapters
        add("$chapterDirName.cbz")

        if (chapterScanlator.isNullOrBlank()) {
            // Previously null scanlator fields were converted to "" due to a bug
            add("_$chapterDirName")
            add("_$chapterDirName.cbz")
        } else {
            // Legacy chapter directory name used in v0.9.2 and before
            add(DiskUtil.buildValidFilename(chapterName))
        }

        // any legacy names
        legacyChapterDirNames.forEach {
            add(it)
            add("$it.cbz")
        }
    }
}
