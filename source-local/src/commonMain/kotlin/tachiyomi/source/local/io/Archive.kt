package tachiyomi.source.local.io

import com.hippo.unifile.UniFile
import tachiyomi.core.common.storage.extension

/** The archive types the local source can read chapters from. */
public object Archive {

    private val SUPPORTED_ARCHIVE_TYPES = listOf("zip", "cbz", "rar", "cbr", "7z", "cb7", "tar", "cbt")

    /** Whether [file]'s extension is one of the supported archive types. */
    public fun isSupported(file: UniFile): Boolean = file.extension?.lowercase() in SUPPORTED_ARCHIVE_TYPES
}
