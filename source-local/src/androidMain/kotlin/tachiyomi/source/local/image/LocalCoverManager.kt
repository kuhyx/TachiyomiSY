package tachiyomi.source.local.image

import android.content.Context
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.storage.DiskUtil
import mihon.core.common.archive.ZipWriter
import tachiyomi.core.common.storage.nameWithoutExtension
import tachiyomi.core.common.util.system.ImageUtil
import tachiyomi.source.local.io.LocalSourceFileSystem
import java.io.InputStream

private const val DEFAULT_COVER_NAME = "cover.jpg"
private const val COVER_ARCHIVE_NAME = "cover.cbi"

/** Finds and writes the `cover.*` file of a local manga folder. */
public actual class LocalCoverManager(
    private val context: Context,
    private val fileSystem: LocalSourceFileSystem,
) {

    /** The first image file called `cover` (or the encrypted `cover.cbi`) in the manga folder. */
    public actual fun find(mangaUrl: String): UniFile? = fileSystem.getFilesInMangaDirectory(mangaUrl)
        // Get all file whose names start with "cover"
        .filter { it.isFile && it.nameWithoutExtension.equals("cover", ignoreCase = true) }
        // Get the first actual image
        .firstOrNull {
            ImageUtil.isImage(it.name) { it.openInputStream() } || it.name == COVER_ARCHIVE_NAME
        }

    /**
     * Writes [inputStream] as the manga's cover, encrypted into `cover.cbi` when [encrypted],
     * points [manga]'s thumbnail at it and returns the file; `null` when the folder is missing.
     */
    public actual fun update(manga: SManga, inputStream: InputStream, encrypted: Boolean): UniFile? {
        val directory = fileSystem.getMangaDirectory(manga.url)
        if (directory == null) {
            inputStream.close()
            return null
        }

        val targetFile = checkNotNull(
            find(manga.url) ?: directory.createFile(if (encrypted) COVER_ARCHIVE_NAME else DEFAULT_COVER_NAME),
        )

        inputStream.use { input ->
            if (encrypted) {
                ZipWriter(context, targetFile, encrypt = true).use { writer ->
                    writer.write(input.readBytes(), DEFAULT_COVER_NAME)
                }
            } else {
                targetFile.openOutputStream().use { output -> input.copyTo(output) }
            }
        }
        DiskUtil.createNoMediaFile(directory, context)
        manga.thumbnail_url = targetFile.uri.toString()
        return targetFile
    }
}
