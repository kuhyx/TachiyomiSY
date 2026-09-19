package tachiyomi.source.local

import android.content.Context
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import mihon.core.common.archive.ZipWriter
import mihon.core.common.archive.archiveReader
import nl.adaptivity.xmlutil.core.AndroidXmlReader
import nl.adaptivity.xmlutil.serialization.XML
import tachiyomi.core.metadata.comicinfo.COMIC_INFO_FILE
import tachiyomi.core.metadata.comicinfo.ComicInfo
import tachiyomi.core.metadata.comicinfo.copyFromComicInfo
import tachiyomi.source.local.LocalSource.Companion.COMIC_INFO_ARCHIVE
import java.io.InputStream
import java.nio.charset.StandardCharsets

/** Reads and writes `ComicInfo.xml`, plain or inside the encrypted `ComicInfo.cbm` archive. */
internal class ComicInfoFiles(
    private val context: Context,
    private val xml: XML,
) {

    /** Decodes a `ComicInfo.xml` stream. */
    fun parse(stream: InputStream): ComicInfo =
        AndroidXmlReader(stream, StandardCharsets.UTF_8.name()).use {
            xml.decodeFromReader<ComicInfo>(it)
        }

    /** Serialises [comicInfo] back to `ComicInfo.xml`. */
    fun encode(comicInfo: ComicInfo): String = xml.encodeToString(ComicInfo.serializer(), comicInfo)

    /** Copies the metadata of the `ComicInfo.xml` [stream] onto [manga]. */
    fun applyToManga(stream: InputStream, manga: SManga) {
        manga.copyFromComicInfo(parse(stream))
    }

    /** Copies name, number and scanlator of the `ComicInfo.xml` [stream] onto [chapter]. */
    fun applyToChapter(stream: InputStream, chapter: SChapter) {
        val comicInfo = parse(stream)

        comicInfo.title?.let { chapter.name = it.value }
        comicInfo.number?.let { number -> number.value.toFloatOrNull()?.let { chapter.chapter_number = it } }
        comicInfo.translator?.let { chapter.scanlator = it.value }
    }

    /**
     * Runs [block] over the `ComicInfo.xml` of a chapter (a directory or an archive) with a
     * flag telling whether the source archive was encrypted; `null` when the chapter has none.
     */
    fun <T> forChapter(chapter: UniFile, block: (InputStream, Boolean) -> T): T? =
        if (chapter.isDirectory) {
            chapter.findFile(COMIC_INFO_FILE)?.let { file ->
                file.openInputStream().use { block(it, false) }
            }
        } else {
            chapter.archiveReader(context).use { reader ->
                reader.getInputStream(COMIC_INFO_FILE)?.use { block(it, reader.encrypted) }
            }
        }

    /** Copies the first `ComicInfo.xml` found in [chapterArchives] to [folder]'s top level. */
    fun copyFromChapters(chapterArchives: List<UniFile>, folder: UniFile): UniFile? {
        for (chapter in chapterArchives) {
            val file = forChapter(chapter) { stream, encrypted -> copy(stream, folder, encrypted) }
            if (file != null) return file
        }
        return null
    }

    /**
     * Writes [comicInfoFileStream] into [folder] as `ComicInfo.xml`, or as the encrypted
     * `ComicInfo.cbm` archive when [encrypt] is set, and returns the written file.
     */
    fun copy(comicInfoFileStream: InputStream, folder: UniFile, encrypt: Boolean): UniFile? {
        if (encrypt) {
            val comicInfoArchiveFile = folder.createFile(COMIC_INFO_ARCHIVE)
            comicInfoArchiveFile?.let { archive ->
                ZipWriter(context, archive, encrypt = true).use { writer ->
                    writer.write(comicInfoFileStream.use { it.readBytes() }, COMIC_INFO_FILE)
                }
            }
            return comicInfoArchiveFile
        }
        return folder.createFile(COMIC_INFO_FILE)?.apply {
            openOutputStream().use { outputStream ->
                comicInfoFileStream.use { it.copyTo(outputStream) }
            }
        }
    }
}
