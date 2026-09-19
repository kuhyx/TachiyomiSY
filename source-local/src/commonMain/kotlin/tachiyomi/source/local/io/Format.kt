package tachiyomi.source.local.io

import com.hippo.unifile.UniFile
import tachiyomi.core.common.storage.extension
import tachiyomi.source.local.io.Archive.isSupported as isArchiveSupported

/** How a local chapter is stored on disk. */
public sealed interface Format {
    /** A folder of image files. */
    public data class Directory(
        /** The chapter folder. */
        val file: UniFile,
    ) : Format

    /** A supported archive (see [tachiyomi.source.local.io.Archive]) of image files. */
    public data class Archive(
        /** The archive file. */
        val file: UniFile,
    ) : Format

    /** An EPUB book. */
    public data class Epub(
        /** The EPUB file. */
        val file: UniFile,
    ) : Format

    /** Thrown by [valueOf] for a file that is neither a folder, a supported archive nor an EPUB. */
    public class UnknownFormatException : Exception()

    /** Classifies chapter files. */
    public companion object {

        /**
         * The [Format] of [file].
         *
         * @throws UnknownFormatException when the file is of no supported type.
         */
        public fun valueOf(file: UniFile): Format = when {
            file.isDirectory -> Directory(file)
            file.extension.equals("epub", true) -> Epub(file)
            isArchiveSupported(file) -> Archive(file)
            else -> throw UnknownFormatException()
        }
    }
}
