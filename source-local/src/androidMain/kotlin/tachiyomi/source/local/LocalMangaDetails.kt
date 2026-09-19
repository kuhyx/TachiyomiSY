package tachiyomi.source.local

import android.content.Context
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import logcat.LogPriority
import mihon.core.common.archive.archiveReader
import tachiyomi.core.common.storage.extension
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.core.metadata.comicinfo.COMIC_INFO_FILE
import tachiyomi.core.metadata.comicinfo.getComicInfo
import tachiyomi.core.metadata.tachiyomi.MangaDetails
import tachiyomi.source.local.LocalSource.Companion.COMIC_INFO_ARCHIVE
import tachiyomi.source.local.image.LocalCoverManager
import tachiyomi.source.local.io.Archive
import tachiyomi.source.local.io.LocalSourceFileSystem

private const val NO_XML_FILE = ".noxml"

/** Fills a manga's details from the metadata files of its directory. */
internal class LocalMangaDetails(
    private val context: Context,
    private val fileSystem: LocalSourceFileSystem,
    private val coverManager: LocalCoverManager,
    private val json: Json,
    private val comicInfoFiles: ComicInfoFiles,
) {

    /**
     * Sets the cover and, in order of preference, the details from a top-level `ComicInfo.xml`,
     * an encrypted `ComicInfo.cbm`, the legacy `details.json` (migrated to `ComicInfo.xml`), or
     * the `ComicInfo.xml` of the first chapter archive that has one (copied to the top level).
     * A `.noxml` marker records that the chapters were scanned and had none.
     */
    suspend fun fetch(manga: SManga): SManga = withIOContext {
        coverManager.find(manga.url)?.let {
            manga.thumbnail_url = it.uri.toString()
        }

        // Augment manga details based on metadata files
        try {
            val mangaDir = fileSystem.getMangaDirectory(manga.url) ?: error("${manga.url} is not a valid directory")
            fillFromMetadataFiles(mangaDir, manga)
        } catch (expected: Throwable) {
            logcat(LogPriority.ERROR, expected) { "Error setting manga details from local metadata for ${manga.title}" }
        }

        manga
    }

    private fun fillFromMetadataFiles(mangaDir: UniFile, manga: SManga) {
        val mangaDirFiles = mangaDir.listFiles().orEmpty().toList()
        val comicInfoFile = mangaDirFiles.firstOrNull { it.name == COMIC_INFO_FILE }
        val noXmlFile = mangaDirFiles.firstOrNull { it.name == NO_XML_FILE }
        val legacyJsonDetailsFile = mangaDirFiles.firstOrNull { it.extension == "json" }
        val comicInfoArchiveFile = mangaDirFiles.firstOrNull { it.name == COMIC_INFO_ARCHIVE }

        when {
            // Top level ComicInfo.xml
            comicInfoFile != null -> {
                noXmlFile?.delete()
                comicInfoFiles.applyToManga(comicInfoFile.openInputStream(), manga)
            }
            comicInfoArchiveFile != null -> {
                noXmlFile?.delete()
                applyArchived(comicInfoArchiveFile, manga)
            }
            // Old custom JSON format
            legacyJsonDetailsFile != null -> {
                migrateLegacyJson(legacyJsonDetailsFile, mangaDir, manga)
            }
            // Copy ComicInfo.xml from chapter archive to top level if found
            noXmlFile == null -> {
                copyFromChapters(mangaDirFiles, mangaDir, manga)
            }
        }
    }

    private fun applyArchived(comicInfoArchiveFile: UniFile, manga: SManga) {
        comicInfoArchiveFile.archiveReader(context).getInputStream(COMIC_INFO_FILE)
            ?.let { comicInfoFiles.applyToManga(it, manga) }
    }

    private fun migrateLegacyJson(legacyJsonDetailsFile: UniFile, mangaDir: UniFile, manga: SManga) {
        json.decodeFromStream<MangaDetails>(legacyJsonDetailsFile.openInputStream()).run {
            title?.let { manga.title = it }
            author?.let { manga.author = it }
            artist?.let { manga.artist = it }
            description?.let { manga.description = it }
            genre?.let { manga.genre = it.joinToString() }
            status?.let { manga.status = it }
        }
        // Replace with ComicInfo.xml file
        val comicInfo = manga.getComicInfo()
        mangaDir.createFile(COMIC_INFO_FILE)?.let { file ->
            file.openOutputStream().use {
                it.write(comicInfoFiles.encode(comicInfo).toByteArray())
                legacyJsonDetailsFile.delete()
            }
        }
    }

    private fun copyFromChapters(mangaDirFiles: List<UniFile>, mangaDir: UniFile, manga: SManga) {
        val chapterArchives = mangaDirFiles.filter(Archive::isSupported)
        val copiedFile = comicInfoFiles.copyFromChapters(chapterArchives, mangaDir)
        when {
            copiedFile == null -> mangaDir.createFile(NO_XML_FILE) // Avoid re-scanning
            copiedFile.name == COMIC_INFO_ARCHIVE -> applyArchived(copiedFile, manga)
            else -> comicInfoFiles.applyToManga(copiedFile.openInputStream(), manga)
        }
    }
}
