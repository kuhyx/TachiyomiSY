package eu.kanade.domain

import logcat.LogPriority
import logcat.LogcatLogger

/** Installs a logcat logger that collects every message into the returned list; pair with [releaseLogcat]. */
internal fun captureLogcat(): MutableList<String> {
    val logged = mutableListOf<String>()
    LogcatLogger.install()
    LogcatLogger.loggers += object : LogcatLogger {
        @Deprecated("Superseded by the tagged overload", ReplaceWith("isLoggable(priority, \"\")"))
        override fun isLoggable(priority: LogPriority): Boolean = true

        override fun isLoggable(priority: LogPriority, tag: String): Boolean = true

        override fun log(priority: LogPriority, tag: String, message: String) {
            logged += message
        }
    }
    return logged
}

/** Removes the logger installed by [captureLogcat]. */
internal fun releaseLogcat() {
    LogcatLogger.uninstall()
}
