package tachiyomi.core.common.storage

import java.io.File

public interface FolderProvider {

    public fun directory(): File

    public fun path(): String
}
