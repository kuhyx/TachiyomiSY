package eu.kanade.tachiyomi.data.download

import com.hippo.unifile.UniFile
import tachiyomi.core.common.storage.extension
import tachiyomi.core.common.storage.nameWithoutExtension

// The named subdirectories of a directory that may not exist.
internal fun UniFile?.namedSubDirectories(): List<UniFile> =
    this?.listFiles().orEmpty().filter { it.isDirectory && !it.name.isNullOrBlank() }

// Every chapter under a manga directory: image folders and CBZ files; incomplete downloads are ignored.
internal fun UniFile?.chapterDirNames(): MutableSet<String> = this?.listFiles().orEmpty()
    .mapNotNull {
        when {
            it.name?.endsWith(Downloader.TMP_DIR_SUFFIX) == true -> null
            it.isDirectory -> it.name
            it.isFile && it.extension == "cbz" -> it.nameWithoutExtension
            else -> null
        }
    }
    .toMutableSet()

// Reads the manga directories of a source and the chapters under each.
internal fun SourceDirectory.scan() {
    mangaDirs = dir.namedSubDirectories().associate { it.name!! to MangaDirectory(it) }
    mangaDirs.values.forEach { mangaDir ->
        mangaDir.chapterDirs = mangaDir.dir.chapterDirNames()
    }
}
