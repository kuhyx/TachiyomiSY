package tachiyomi.source.local

import android.content.Context
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.lang.compareNaturalIgnoreCase
import eu.kanade.tachiyomi.util.storage.EpubFile
import logcat.LogPriority
import mihon.core.common.archive.archiveReader
import tachiyomi.core.common.storage.extension
import tachiyomi.core.common.storage.nameWithoutExtension
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.ImageUtil
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.chapter.service.ChapterRecognition
import tachiyomi.source.local.image.LocalCoverManager
import tachiyomi.source.local.io.Archive
import tachiyomi.source.local.io.Format
import tachiyomi.source.local.io.LocalSourceFileSystem
import tachiyomi.source.local.metadata.fillMetadata

/** Lists the chapters of a local manga directory and takes its cover from a chapter if needed. */
internal class LocalChapterLister(
    private val context: Context,
    private val fileSystem: LocalSourceFileSystem,
    private val coverManager: LocalCoverManager,
    private val comicInfoFiles: ComicInfoFiles,
    private val formats: LocalChapterFormats,
) {

    /**
     * Every directory, supported archive or EPUB under the manga directory as a chapter, newest
     * name first, with the number parsed from the name and the metadata read from the file.
     * When [manga] has no cover yet the first chapter's first image becomes it.
     */
    suspend fun list(manga: SManga): List<SChapter> = withIOContext {
        val chapters = fileSystem.getFilesInMangaDirectory(manga.url)
            // Only keep supported formats
            .filterNot { it.name.orEmpty().startsWith('.') }
            .filter { it.isDirectory || Archive.isSupported(it) || it.extension.equals("epub", true) }
            .map { chapterFile -> chapterOf(chapterFile, manga) }
            .sortedWith { c1, c2 -> c2.name.compareNaturalIgnoreCase(c1.name) }

        // Copy the cover from the first chapter found if not available
        if (manga.thumbnail_url.isNullOrBlank()) {
            chapters.lastOrNull()?.let { chapter -> updateCover(chapter, manga) }
        }

        chapters
    }

    private fun chapterOf(chapterFile: UniFile, manga: SManga): SChapter = SChapter.create().apply {
        url = "${manga.url}/${chapterFile.name}"
        name = (if (chapterFile.isDirectory) chapterFile.name else chapterFile.nameWithoutExtension).orEmpty()
        date_upload = chapterFile.lastModified()
        chapter_number = ChapterRecognition
            .parseChapterNumber(manga.title, this.name, this.chapter_number.toDouble())
            .toFloat()

        val format = Format.valueOf(chapterFile)
        if (format is Format.Epub) {
            EpubFile(format.file.archiveReader(context)).use { epub -> epub.fillMetadata(manga, this) }
        } else {
            comicInfoFiles.forChapter(chapterFile) { stream, _ -> comicInfoFiles.applyToChapter(stream, this) }
        }
    }

    private fun updateCover(chapter: SChapter, manga: SManga): UniFile? = try {
        when (val format = formats.resolve(chapter)) {
            is Format.Directory -> coverFromDirectory(format, manga)
            is Format.Archive -> coverFromArchive(format, manga)
            is Format.Epub -> coverFromEpub(format, manga)
        }
    } catch (expected: Throwable) {
        logcat(LogPriority.ERROR, expected) { "Error updating cover for ${manga.title}" }
        null
    }

    private fun coverFromDirectory(format: Format.Directory, manga: SManga): UniFile? {
        val entry = format.file.listFiles()
            ?.sortedWith { f1, f2 -> f1.name.orEmpty().compareNaturalIgnoreCase(f2.name.orEmpty()) }
            ?.find { !it.isDirectory && ImageUtil.isImage(it.name) { it.openInputStream() } }
        return entry?.let { coverManager.update(manga, it.openInputStream()) }
    }

    private fun coverFromArchive(format: Format.Archive, manga: SManga): UniFile? =
        format.file.archiveReader(context).use { reader ->
            val entry = reader.useEntries { entries ->
                entries
                    .sortedWith { f1, f2 -> f1.name.compareNaturalIgnoreCase(f2.name) }
                    .find { it.isFile && ImageUtil.isImage(it.name) { checkNotNull(reader.getInputStream(it.name)) } }
            }
            entry?.let { coverManager.update(manga, checkNotNull(reader.getInputStream(it.name)), reader.encrypted) }
        }

    private fun coverFromEpub(format: Format.Epub, manga: SManga): UniFile? =
        EpubFile(format.file.archiveReader(context)).use { epub ->
            val entry = epub.getImagesFromPages().firstOrNull()
            entry?.let { coverManager.update(manga, checkNotNull(epub.getInputStream(it))) }
        }
}
