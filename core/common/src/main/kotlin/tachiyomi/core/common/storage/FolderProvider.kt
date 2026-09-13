package tachiyomi.core.common.storage

import java.io.File

/** Supplies the root folder the app stores data in. */
public interface FolderProvider {

    /** The folder. */
    public fun directory(): File

    /** The folder's absolute path. */
    public fun path(): String
}
