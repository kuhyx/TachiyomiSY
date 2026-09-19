package tachiyomi.source.local

import android.content.Context
import eu.kanade.tachiyomi.source.model.SManga
import mihon.core.common.archive.archiveReader
import tachiyomi.core.metadata.comicinfo.COMIC_INFO_FILE
import tachiyomi.core.metadata.comicinfo.ComicInfo
import tachiyomi.core.metadata.comicinfo.ComicInfoPublishingStatus
import tachiyomi.core.metadata.comicinfo.getComicInfo
import tachiyomi.source.local.LocalSource.Companion.COMIC_INFO_ARCHIVE
import tachiyomi.source.local.io.LocalSourceFileSystem

/** Writes edited manga details back into the manga directory's `ComicInfo.xml`. */
internal class LocalMangaInfoWriter(
    private val context: Context,
    private val fileSystem: LocalSourceFileSystem,
    private val comicInfoFiles: ComicInfoFiles,
) {

    /**
     * Merges [manga]'s editable fields into the existing `ComicInfo.xml` (or `.cbm`) of its
     * directory, creating the file from scratch when there is none; encryption is preserved.
     */
    fun write(manga: SManga) {
        val mangaDirFiles = fileSystem.getFilesInMangaDirectory(manga.url)
        val existingFile = mangaDirFiles.firstOrNull { it.name == COMIC_INFO_FILE }
        val comicInfoArchiveFile = mangaDirFiles.firstOrNull { it.name == COMIC_INFO_ARCHIVE }
        val comicInfoArchiveReader = comicInfoArchiveFile?.archiveReader(context)
        val existingComicInfo =
            (existingFile?.openInputStream() ?: comicInfoArchiveReader?.getInputStream(COMIC_INFO_FILE))
                ?.use(comicInfoFiles::parse)
        val newComicInfo = existingComicInfo?.mergedWith(manga) ?: manga.getComicInfo()

        fileSystem.getMangaDirectory(manga.url)?.let {
            comicInfoFiles.copy(
                comicInfoFiles.encode(newComicInfo).byteInputStream(),
                it,
                comicInfoArchiveReader?.encrypted ?: false,
            )
        }
    }

    private fun ComicInfo.mergedWith(manga: SManga): ComicInfo = manga.run {
        copy(
            series = ComicInfo.Series(title),
            summary = description?.let { ComicInfo.Summary(it) },
            writer = author?.let { ComicInfo.Writer(it) },
            penciller = artist?.let { ComicInfo.Penciller(it) },
            genre = genre?.let { ComicInfo.Genre(it) },
            publishingStatus = ComicInfo.PublishingStatusTachiyomi(
                ComicInfoPublishingStatus.toComicInfoValue(status.toLong()),
            ),
        )
    }
}
