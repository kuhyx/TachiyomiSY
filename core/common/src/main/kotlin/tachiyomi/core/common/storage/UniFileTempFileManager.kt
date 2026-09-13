package tachiyomi.core.common.storage

import android.content.Context
import android.os.Build
import android.os.FileUtils
import com.hippo.unifile.UniFile
import java.io.BufferedOutputStream
import java.io.File

// File.createTempFile rejects prefixes shorter than three characters.
private const val MIN_PREFIX_LENGTH = 3
private const val COPY_BUFFER_BYTES = 8192

/** Copies content-URI files into the cache so native code can read them by path. */
public class UniFileTempFileManager(
    private val context: Context,
) {

    private val dir = File(context.externalCacheDir, "tmp")

    /** A cached copy of [file]. */
    public fun createTempFile(file: UniFile): File {
        dir.mkdirs()

        val inputStream = context.contentResolver.openInputStream(file.uri)!!
        val tempFile = File.createTempFile(
            file.nameWithoutExtension.orEmpty().padEnd(MIN_PREFIX_LENGTH),
            null,
            dir,
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            FileUtils.copy(inputStream, tempFile.outputStream())
        } else {
            BufferedOutputStream(tempFile.outputStream()).use { tmpOut ->
                inputStream.use { input ->
                    val buffer = ByteArray(COPY_BUFFER_BYTES)
                    var count: Int
                    while (input.read(buffer).also { count = it } > 0) {
                        tmpOut.write(buffer, 0, count)
                    }
                }
            }
        }

        return tempFile
    }

    /** Removes every cached copy. */
    public fun deleteTempFiles() {
        dir.deleteRecursively()
    }
}
