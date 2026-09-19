package tachiyomi.source.local.io

import com.hippo.unifile.UniFile

/** The local-source directory: one sub-folder per manga. */
public expect class LocalSourceFileSystem {

    /** The local-source directory, or `null` while no storage location is set. */
    public fun getBaseDirectory(): UniFile?

    /** Everything directly under the local-source directory. */
    public fun getFilesInBaseDirectory(): List<UniFile>

    /** The folder of the manga called [name], or `null` when it is missing or not a folder. */
    public fun getMangaDirectory(name: String): UniFile?

    /** Everything directly under the folder of the manga called [name]; empty when it is missing. */
    public fun getFilesInMangaDirectory(name: String): List<UniFile>
}
