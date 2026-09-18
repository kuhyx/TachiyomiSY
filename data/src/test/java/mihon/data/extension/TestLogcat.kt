package mihon.data.extension

import logcat.LogPriority
import logcat.LogcatLogger

/**
 * A [LogcatLogger] that records every line, so the message lambdas passed to `logcat { }` run
 * (they are skipped while no logger is installed). Added once per class loader:
 * `LogcatLogger.uninstall` only flips a flag and never removes loggers.
 */
internal object TestLogcat : LogcatLogger {
    /** Every message logged since [start], oldest first. */
    val messages: MutableList<String> = mutableListOf()

    /** Installs the logger (if needed) and forgets earlier lines. */
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
