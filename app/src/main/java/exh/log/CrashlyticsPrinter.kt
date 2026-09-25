package exh.log

import com.elvishew.xlog.printer.Printer
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import eu.kanade.tachiyomi.BuildConfig

internal class CrashlyticsPrinter(private val logLevel: Int) : Printer {
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
                crashOnDebug(expected)
            }
        }
    }
}

/** Rethrows [cause] in debug builds; a release build swallows a Crashlytics failure. */
internal fun crashOnDebug(cause: Throwable, isDebug: Boolean = BuildConfig.DEBUG) {
    if (isDebug) throw cause
}
