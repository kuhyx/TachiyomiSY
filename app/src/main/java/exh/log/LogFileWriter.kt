package exh.log

import com.hippo.unifile.UniFile
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.io.BufferedWriter
import java.io.IOException

/**
 * Used to write the flattened logs to the log file.
 */
internal class LogFileWriter {
    /**
     * Get the name of last used log file.
     * @return the name of last used log file, maybe null
     */
    var lastFileName: String? = null
        private set

    /**
     * Get the current log file.
     *
     * @return the current log file, maybe null
     */
    var file: UniFile? = null
        private set

    private var bufferedWriter: BufferedWriter? = null

    /**
     * Whether the log file is opened.
     *
     * @return true if opened, false otherwise
     */
    val isOpened: Boolean
        get() = bufferedWriter != null

    /**
     * Open the file of specific name to be written into.
     *
     * @param file the file to write into
     * @return true if opened successfully, false otherwise
     */
    fun open(file: UniFile): Boolean {
        return try {
            bufferedWriter = file.openOutputStream().bufferedWriter()
            lastFileName = file.name
            this.file = file
            true
        } catch (expected: Exception) {
            // Logged whatever the cause; the printer reports that it could not open the file.
            logcat(LogPriority.ERROR, expected) { "Could not open the log file" }
            false
        }
    }

    /**
     * Close the current log file if it is opened.
     *
     * @return true if closed successfully, false otherwise
     */
    fun close(): Boolean {
        if (bufferedWriter != null) {
            try {
                bufferedWriter?.close()
            } catch (failed: IOException) {
                logcat(LogPriority.ERROR, failed) { "Could not close the log file" }
                return false
            } finally {
                bufferedWriter = null
                lastFileName = null
                file = null
            }
        }
        return true
    }

    /**
     * Append the flattened log to the end of current opened log file.
     *
     * @param flattenedLog the flattened log
     */
    fun appendLog(flattenedLog: String) {
        val bufferedWriter = bufferedWriter
        requireNotNull(bufferedWriter)
        try {
            bufferedWriter.write(flattenedLog)
            bufferedWriter.newLine()
            bufferedWriter.flush()
        } catch (_: IOException) {
            // A failed write is dropped: the printer must never throw back into the logger.
        }
    }
}
