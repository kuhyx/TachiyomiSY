package tachiyomi.source.local

import logcat.LogPriority
import logcat.LogcatLogger

/** Captures every logcat line so error paths can be asserted and their message lambdas actually run. */
internal object RecordingLogger : LogcatLogger {
    /** Every message logged since [start], oldest first. */
    val messages: MutableList<String> = mutableListOf()

    /** Installs the logger if needed and forgets earlier lines. */
    fun start() {
        if (!LogcatLogger.isInstalled) LogcatLogger.install()
        if (this !in LogcatLogger.loggers) LogcatLogger.loggers.add(this)
        messages.clear()
    }

    override fun isLoggable(priority: LogPriority, tag: String): Boolean = true

    override fun log(priority: LogPriority, tag: String, message: String) {
        messages += message
    }
}
