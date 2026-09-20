package eu.kanade.tachiyomi.data.download

import com.hippo.unifile.UniFile
import eu.kanade.domain.chapter.model.toSChapter
import eu.kanade.domain.manga.model.getComicInfo
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.util.storage.CbzCrypto
import mihon.core.common.archive.ZipWriter
import tachiyomi.core.metadata.comicinfo.COMIC_INFO_FILE
import tachiyomi.core.metadata.comicinfo.ComicInfo
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga
import uy.kohesive.injekt.api.get

// Archive the chapter pages as a CBZ.
internal fun Downloader.archiveChapter(
    mangaDir: UniFile,
    dirname: String,
    tmpDir: UniFile,
) {
    // SY -->
    val encrypt = CbzCrypto.getPasswordProtectDlPref() && CbzCrypto.isPasswordSet()
    // SY <--

    val zip = mangaDir.createFile("$dirname.cbz${Downloader.TMP_DIR_SUFFIX}")!!
    ZipWriter(context, zip, /* SY --> */ encrypt /* SY <-- */).use { writer ->
        tmpDir.listFiles()?.forEach { file ->
            writer.write(file)
        }
    }
    zip.renameTo("$dirname.cbz")
    tmpDir.delete()
}

// Creates a ComicInfo.xml file inside the given directory.
internal suspend fun Downloader.createComicInfoFile(
    dir: UniFile,
    manga: Manga,
    chapter: Chapter,
    source: HttpSource,
) {
    val categories = getCategories.await(manga.id).map { it.name.trim() }.takeUnless { it.isEmpty() }
    val urls = getTracks.await(manga.id)
        .mapNotNull { track ->
            track.remoteUrl.takeUnless { url -> url.isBlank() }?.trim()
        }
        .plus(source.getChapterUrl(chapter.toSChapter()).trim())
        .distinct()

    val comicInfo = getComicInfo(
        manga,
        chapter,
        urls,
        categories,
        source.name,
    )

    // Remove the old file
    dir.findFile(COMIC_INFO_FILE)?.delete()
    dir.createFile(COMIC_INFO_FILE)!!.openOutputStream().use {
        val comicInfoString = xml.encodeToString(ComicInfo.serializer(), comicInfo)
        it.write(comicInfoString.toByteArray())
    }
}
