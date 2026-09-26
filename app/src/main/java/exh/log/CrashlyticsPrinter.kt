package exh.log

import com.elvishew.xlog.printer.Printer
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import eu.kanade.tachiyomi.BuildConfig

// [isDebug] is BuildConfig.DEBUG: a debug build rethrows, so its catch never completes normally.
internal class CrashlyticsPrinter(
    private val logLevel: Int,
    private val isDebug: Boolean = BuildConfig.DEBUG,
) : Printer {
    /**
     * Print log in new line.
     *
     * @param logLevel the level of log
     * @param tag the tag of log
     * @param msg the msg of log
     */
    override fun println(logLevel: Int, tag: String?, msg: String?) {
        if (logLevel >= this.logLevel) {
            try {
                Firebase.crashlytics.log("$logLevel/$tag: $msg")
            } catch (expected: Throwable) {
                // Crash in debug builds if Crashlytics itself fails; BuildConfig.DEBUG is a
                // compile-time constant, so the check goes through a measurable function.
                crashOnDebug(expected, isDebug)
            }
        }
    }
}

/** Rethrows [cause] in debug builds; a release build swallows a Crashlytics failure. */
internal fun crashOnDebug(cause: Throwable, isDebug: Boolean) {
    if (isDebug) throw cause
}
