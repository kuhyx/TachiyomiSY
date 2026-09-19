package tachiyomi.source.local.io

import com.hippo.unifile.UniFile
import tachiyomi.domain.storage.service.StorageManager

/** The local-source directory: one sub-folder per manga. */
public actual class LocalSourceFileSystem(
    private val storageManager: StorageManager,
) {

    /** The local-source directory, or `null` while no storage location is set. */
    public actual fun getBaseDirectory(): UniFile? = storageManager.getLocalSourceDirectory()

    /** Everything directly under the local-source directory. */
    public actual fun getFilesInBaseDirectory(): List<UniFile> = getBaseDirectory()?.listFiles().orEmpty().toList()

    /** The folder of the manga called [name], or `null` when it is missing or not a folder. */
    public actual fun getMangaDirectory(name: String): UniFile? = getBaseDirectory()
        ?.findFile(name)
        ?.takeIf { it.isDirectory }

    /** Everything directly under the folder of the manga called [name]; empty when it is missing. */
    public actual fun getFilesInMangaDirectory(name: String): List<UniFile> = getBaseDirectory()
        ?.findFile(name)
        ?.takeIf { it.isDirectory }
        ?.listFiles()
        .orEmpty()
        .toList()
}
