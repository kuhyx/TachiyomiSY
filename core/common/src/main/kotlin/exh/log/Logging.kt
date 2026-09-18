package exh.log

import android.util.Log
import com.elvishew.xlog.Logger
import com.elvishew.xlog.XLog
import com.elvishew.xlog.LogLevel as XLogLevel

/** An XLog logger tagged with the receiver's class name. */
public fun Any.xLog(): Logger = XLog.tag(this::class.java.simpleName).build()

/** Like [xLog] but with stack traces enabled. */
public fun Any.xLogStack(): Logger = XLog.tag(this::class.java.simpleName).enableStackTrace(0).build()

/**
 * A log level mapped onto both XLog and Android priorities.
 *
 * @property int the XLog level.
 * @property androidLevel the matching android.util.Log priority.
 */
public sealed class LogLevel(public val int: Int, public val androidLevel: Int) {
    /** Nothing. */
    public object None : LogLevel(XLogLevel.NONE, Log.ASSERT)

    /** Errors. */
    public object Error : LogLevel(XLogLevel.ERROR, Log.ERROR)

    /** Warnings and up. */
    public object Warn : LogLevel(XLogLevel.WARN, Log.WARN)

    /** Info and up. */
    public object Info : LogLevel(XLogLevel.INFO, Log.INFO)

    /** Debug and up. */
    public object Debug : LogLevel(XLogLevel.DEBUG, Log.DEBUG)

    /** Verbose and up. */
    public object Verbose : LogLevel(XLogLevel.VERBOSE, Log.VERBOSE)

    /** Everything. */
    public object All : LogLevel(XLogLevel.ALL, Log.VERBOSE)

    /** XLog's name of the level. */
    public val name: String get() = getLevelName(this)

    /** XLog's one-letter name of the level. */
    public val shortName: String get() = getLevelShortName(this)

    /** Level names and the ordered list of levels. */
    public companion object {
        /** XLog's name of [logLevel]. */
        public fun getLevelName(logLevel: LogLevel): String = XLogLevel.getLevelName(logLevel.int)

        /** XLog's one-letter name of [logLevel]. */
        public fun getLevelShortName(logLevel: LogLevel): String = XLogLevel.getShortLevelName(logLevel.int)

        /** Every level, most to least restrictive. */
        public fun values(): List<LogLevel> = listOf(
            None,
            Error,
            Warn,
            Info,
            Debug,
            Verbose,
            All,
        )
    }
}
