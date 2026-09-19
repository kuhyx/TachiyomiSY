package tachiyomi.source.local.image

import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.model.SManga
import java.io.InputStream

/** Finds and writes the `cover.*` file of a local manga folder. */
public expect class LocalCoverManager {

    /** The first image file called `cover` (or the encrypted `cover.cbi`) in the manga folder. */
    public fun find(mangaUrl: String): UniFile?

    /**
     * Writes [inputStream] as the manga's cover, encrypted into `cover.cbi` when [encrypted],
     * points [manga]'s thumbnail at it and returns the file; `null` when the folder is missing.
     */
    public fun update(manga: SManga, inputStream: InputStream, encrypted: Boolean = false): UniFile?
}
