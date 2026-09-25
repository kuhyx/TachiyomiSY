package mihon.test

import logcat.LogPriority
import logcat.LogcatLogger

/**
 * Collects every message the code under test logs. Without an installed logger `logcat { ... }`
 * never evaluates its lambda, so every message body would stay uncovered.
 */
internal class CapturingLogcat : LogcatLogger {
    val messages: MutableList<String> = mutableListOf()

    override fun isLoggable(priority: LogPriority, tag: String): Boolean = true

    override fun log(priority: LogPriority, tag: String, message: String) {
        messages += message
    }

    /** Installs this logger, keeping any logger another test installed. */
    fun install(): CapturingLogcat {
        if (!LogcatLogger.isInstalled) {
            LogcatLogger.install()
        }
        LogcatLogger.loggers += this
        return this
    }

    /** Undoes [install]. */
    fun uninstall() {
        LogcatLogger.loggers -= this
        if (LogcatLogger.loggers.isEmpty()) {
            LogcatLogger.uninstall()
        }
    }
}
