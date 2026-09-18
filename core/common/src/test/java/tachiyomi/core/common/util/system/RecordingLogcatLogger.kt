package tachiyomi.core.common.util.system

import logcat.LogPriority
import logcat.LogcatLogger

/**
 * A [LogcatLogger] that keeps every line it receives, so tests can assert on log output and so
 * the message lambdas passed to `logcat { }` actually execute (they are skipped while no logger
 * is installed). Added once per class loader: `LogcatLogger.uninstall` only flips a flag and never
 * removes loggers, so re-adding on every test would duplicate every line.
 */
internal object RecordingLogcatLogger : LogcatLogger {
    /** One entry per logged line, oldest first. */
    val entries: MutableList<LogEntry> = mutableListOf()

    /** Installs the logger (if needed) and forgets earlier lines. */
    fun start(): RecordingLogcatLogger {
        if (!LogcatLogger.isInstalled) LogcatLogger.install()
        if (this !in LogcatLogger.loggers) LogcatLogger.loggers.add(this)
        entries.clear()
        return this
    }

    /** The messages logged so far, oldest first. */
    fun messages(): List<String> = entries.map { it.message }

    override fun isLoggable(priority: LogPriority, tag: String): Boolean = true

    override fun log(priority: LogPriority, tag: String, message: String) {
        entries += LogEntry(priority, tag, message)
    }
}

/** One captured log line. */
internal data class LogEntry(val priority: LogPriority, val tag: String, val message: String)
