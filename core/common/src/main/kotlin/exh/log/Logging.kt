package exh.log

import android.util.Log
import com.elvishew.xlog.Logger
import com.elvishew.xlog.XLog
import com.elvishew.xlog.LogLevel as XLogLevel

/** An XLog logger tagged with the receiver's class name. */
public fun Any.xLog(): Logger = XLog.tag(this::class.java.simpleName).build()

/** Like [xLog] but with stack traces enabled. */
public fun Any.xLogStack(): Logger = XLog.tag(this::class.java.simpleName).enableStackTrace(0).build()

/** Logs [log] at error level. */
public fun Any.xLogE(log: String) {
    xLog().e(log)
}

/** Logs [log] at warning level. */
public fun Any.xLogW(log: String) {
    xLog().w(log)
}

/** Logs [log] at debug level. */
public fun Any.xLogD(log: String) {
    xLog().d(log)
}

/** Logs [log] at info level. */
public fun Any.xLogI(log: String) {
    xLog().i(log)
}

/** Logs [log] at [logLevel]. */
public fun Any.xLog(logLevel: LogLevel, log: String) {
    xLog().log(logLevel.int, log)
}

/** Logs [log] pretty-printed as JSON. */
public fun Any.xLogJson(log: String) {
    xLog().json(log)
}

/** Logs [log] pretty-printed as XML. */
public fun Any.xLogXML(log: String) {
    xLog().xml(log)
}

/** Logs [log] with [e] at error level. */
public fun Any.xLogE(log: String, e: Throwable) {
    xLogStack().e(log, e)
}

/** Logs [log] with [e] at warning level. */
public fun Any.xLogW(log: String, e: Throwable) {
    xLogStack().w(log, e)
}

/** Logs [log] with [e] at debug level. */
public fun Any.xLogD(log: String, e: Throwable) {
    xLogStack().d(log, e)
}

/** Logs [log] with [e] at info level. */
public fun Any.xLogI(log: String, e: Throwable) {
    xLogStack().i(log, e)
}

/** Logs [log] with [e] at [logLevel]. */
public fun Any.xLog(logLevel: LogLevel, log: String, e: Throwable) {
    xLogStack().log(logLevel.int, log, e)
}

/** Logs [log] (or `null`) at error level. */
public fun Any.xLogE(log: Any?) {
    xLog().let { if (log == null) it.e("null") else it.e(log) }
}

/** Logs [log] (or `null`) at warning level. */
public fun Any.xLogW(log: Any?) {
    xLog().let { if (log == null) it.w("null") else it.w(log) }
}

/** Logs [log] (or `null`) at debug level. */
public fun Any.xLogD(log: Any?) {
    xLog().let { if (log == null) it.d("null") else it.d(log) }
}

/** Logs [log] (or `null`) at info level. */
public fun Any.xLogI(log: Any?) {
    xLog().let { if (log == null) it.i("null") else it.i(log) }
}

/** Logs [log] (or `null`) at [logLevel]. */
public fun Any.xLog(
    logLevel: LogLevel,
    log: Any?,
) {
    xLog().let { if (log == null) it.log(logLevel.int, "null") else it.log(logLevel.int, log) }
}

/*fun Any.xLogE(vararg logs: Any) = xLog().e(logs)
fun Any.xLogW(vararg logs: Any) = xLog().w(logs)
fun Any.xLogD(vararg logs: Any) = xLog().d(logs)
fun Any.xLogI(vararg logs: Any) = xLog().i(logs)
fun Any.xLog(logLevel: LogLevel, vararg logs: Any) = xLog().log(logLevel.int, logs)*/

/** Logs [format] filled with [args] at error level. */
public fun Any.xLogE(format: String, vararg args: Any?) {
    xLog().e(format, *args)
}

/** Logs [format] filled with [args] at warning level. */
public fun Any.xLogW(format: String, vararg args: Any?) {
    xLog().w(format, *args)
}

/** Logs [format] filled with [args] at debug level. */
public fun Any.xLogD(format: String, vararg args: Any?) {
    xLog().d(format, *args)
}

/** Logs [format] filled with [args] at info level. */
public fun Any.xLogI(format: String, vararg args: Any?) {
    xLog().i(format, *args)
}

/** Logs [format] filled with [args] at [logLevel]. */
public fun Any.xLog(logLevel: LogLevel, format: String, vararg args: Any) {
    xLog().log(logLevel.int, format, *args)
}

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

/** Logs [log] with its stack trace at error level. */
@Deprecated("Use proper throwable function", ReplaceWith("""xLogE("", log)"""))
public fun Any.xLogE(log: Throwable) {
    xLogStack().e(log)
}

/** Logs [log] with its stack trace at warning level. */
@Deprecated("Use proper throwable function", ReplaceWith("""xLogW("", log)"""))
public fun Any.xLogW(log: Throwable) {
    xLogStack().w(log)
}

/** Logs [log] with its stack trace at debug level. */
@Deprecated("Use proper throwable function", ReplaceWith("""xLogD("", log)"""))
public fun Any.xLogD(log: Throwable) {
    xLogStack().d(log)
}

/** Logs [log] with its stack trace at info level. */
@Deprecated("Use proper throwable function", ReplaceWith("""xLogI("", log)"""))
public fun Any.xLogI(log: Throwable) {
    xLogStack().i(log)
}

/** Logs [log] with its stack trace at [logLevel]. */
@Deprecated("Use proper throwable function", ReplaceWith("""xLog(logLevel, "", log)"""))
public fun Any.xLog(logLevel: LogLevel, log: Throwable) {
    xLogStack().log(logLevel.int, log)
}
