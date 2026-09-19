package tachiyomi.source.local

import android.content.Context
import eu.kanade.tachiyomi.source.model.SChapter
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import tachiyomi.source.local.io.Format
import tachiyomi.source.local.io.LocalSourceFileSystem

/** Resolves a chapter's URL (`<manga dir>/<chapter file>`) to the file it is stored as. */
internal class LocalChapterFormats(
    private val context: Context,
    private val fileSystem: LocalSourceFileSystem,
) {

    /**
     * The directory, archive or EPUB behind [chapter].
     *
     * @throws IllegalStateException with a user-facing message when the file is missing or of
     * an unsupported type.
     */
    fun resolve(chapter: SChapter): Format {
        val (mangaDirName, chapterName) = chapter.url.split('/', limit = 2)
        val file = fileSystem.getBaseDirectory()
            ?.findFile(mangaDirName)
            ?.findFile(chapterName)
            ?: error(context.stringResource(MR.strings.chapter_not_found))
        return try {
            Format.valueOf(file)
        } catch (e: Format.UnknownFormatException) {
            throw IllegalStateException(context.stringResource(MR.strings.local_invalid_format), e)
        }
    }
}
