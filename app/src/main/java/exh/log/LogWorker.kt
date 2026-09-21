package exh.log

import exh.log.EnhancedFilePrinter.LogItem
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

/**
 * Work in background, we can enqueue the logs, and the worker will dispatch them.
 */
internal class LogWorker(private val printer: EnhancedFilePrinter) : Runnable {
    private val logs: BlockingQueue<LogItem> = LinkedBlockingQueue()

    @Volatile
    private var started = false

    /**
     * Enqueue the log.
     *
     * @param log the log to be written to file
     */
    fun enqueue(log: LogItem) {
        try {
            logs.put(log)
        } catch (interrupted: InterruptedException) {
            logcat(LogPriority.WARN, interrupted) { "Log queue interrupted" }
        }
    }

    /**
     * Whether the worker is started.
     *
     * @return true if started, false otherwise
     */
    fun isStarted(): Boolean {
        synchronized(this) { return started }
    }

    /**
     * Start the worker.
     */
    fun start() {
        synchronized(this) {
            Thread(this).start()
            started = true
        }
    }

    override fun run() {
        try {
            var log: LogItem
            while (logs.take().also { log = it } != null) {
                printer.doPrintln(log.timeMillis, log.level, log.tag, log.msg)
            }
        } catch (interrupted: InterruptedException) {
            logcat(LogPriority.WARN, interrupted) { "Log writer interrupted" }
            synchronized(this) { started = false }
        }
    }
}
