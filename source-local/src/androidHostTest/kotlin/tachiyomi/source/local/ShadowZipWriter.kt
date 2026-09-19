package tachiyomi.source.local

import android.content.Context
import com.hippo.unifile.UniFile
import mihon.core.common.archive.ZipWriter
import org.robolectric.annotation.ClassName
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

/** Package Robolectric must instrument for [ShadowZipWriter] to take over the JNI-backed writer. */
internal const val ZIP_WRITER_PACKAGE: String = "mihon.core.common.archive"

/**
 * Stands in for [ZipWriter], whose constructor opens libarchive and the AndroidKeyStore and so
 * cannot run on the JVM: the shadowed constructor skips the real body and the writer records what
 * it is asked to write. Requires
 * `@Config(instrumentedPackages = [ZIP_WRITER_PACKAGE], shadows = [ShadowZipWriter::class])`.
 */
@Implements(ZipWriter::class)
internal class ShadowZipWriter {
    /** The context the writer was opened with. */
    lateinit var context: Context

    /** The native binding the writer was opened with. */
    lateinit var native: Any

    @Implementation
    fun __constructor__(
        context: Context,
        file: UniFile,
        encrypt: Boolean,
        @ClassName(NATIVE_CLASS) native: Any,
    ) {
        this.context = context
        this.native = native
        lastTarget = file
        calls += "open encrypt=$encrypt"
    }

    @Implementation
    fun write(fileData: ByteArray, fileName: String) {
        calls += "write $fileName ${fileData.size}"
    }

    @Implementation
    fun close() {
        calls += "close"
    }

    companion object {
        private const val NATIVE_CLASS = "mihon.core.common.archive.ArchiveNative"

        /** Every open, write and close across all instances, oldest first. */
        val calls: MutableList<String> = mutableListOf()

        /** The file the most recent writer was opened on. */
        var lastTarget: UniFile? = null

        /** Forgets earlier writers. */
        fun reset() {
            calls.clear()
            lastTarget = null
        }
    }
}
