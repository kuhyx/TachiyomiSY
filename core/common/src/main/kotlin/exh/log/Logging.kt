package exh.log

import android.util.Log
import com.elvishew.xlog.Logger
import com.elvishew.xlog.XLog
import com.elvishew.xlog.LogLevel as XLogLevel

public fun Any.xLog(): Logger = XLog.tag(this::class.java.simpleName).build()

public fun Any.xLogStack(): Logger = XLog.tag(this::class.java.simpleName).enableStackTrace(0).build()

public fun Any.xLogE(log: String) {
    xLog().e(log)
}
public fun Any.xLogW(log: String) {
    xLog().w(log)
}
public fun Any.xLogD(log: String) {
    xLog().d(log)
}
public fun Any.xLogI(log: String) {
    xLog().i(log)
}
public fun Any.xLog(logLevel: LogLevel, log: String) {
    xLog().log(logLevel.int, log)
}
public fun Any.xLogJson(log: String) {
    xLog().json(log)
}
public fun Any.xLogXML(log: String) {
    xLog().xml(log)
}

public fun Any.xLogE(log: String, e: Throwable) {
    xLogStack().e(log, e)
}
public fun Any.xLogW(log: String, e: Throwable) {
    xLogStack().w(log, e)
}
public fun Any.xLogD(log: String, e: Throwable) {
    xLogStack().d(log, e)
}
public fun Any.xLogI(log: String, e: Throwable) {
    xLogStack().i(log, e)
}
public fun Any.xLog(logLevel: LogLevel, log: String, e: Throwable) {
    xLogStack().log(logLevel.int, log, e)
}

public fun Any.xLogE(log: Any?) {
    xLog().let { if (log == null) it.e("null") else it.e(log) }
}
public fun Any.xLogW(log: Any?) {
    xLog().let { if (log == null) it.w("null") else it.w(log) }
}
public fun Any.xLogD(log: Any?) {
    xLog().let { if (log == null) it.d("null") else it.d(log) }
}
public fun Any.xLogI(log: Any?) {
    xLog().let { if (log == null) it.i("null") else it.i(log) }
}
public fun Any.xLog(
    logLevel: LogLevel,
    log: Any?,
): Unit = xLog().let { if (log == null) it.log(logLevel.int, "null") else it.log(logLevel.int, log) }

/*fun Any.xLogE(vararg logs: Any) = xLog().e(logs)
fun Any.xLogW(vararg logs: Any) = xLog().w(logs)
fun Any.xLogD(vararg logs: Any) = xLog().d(logs)
fun Any.xLogI(vararg logs: Any) = xLog().i(logs)
fun Any.xLog(logLevel: LogLevel, vararg logs: Any) = xLog().log(logLevel.int, logs)*/

public fun Any.xLogE(format: String, vararg args: Any?) {
    xLog().e(format, *args)
}
public fun Any.xLogW(format: String, vararg args: Any?) {
    xLog().w(format, *args)
}
public fun Any.xLogD(format: String, vararg args: Any?) {
    xLog().d(format, *args)
}
public fun Any.xLogI(format: String, vararg args: Any?) {
    xLog().i(format, *args)
}
public fun Any.xLog(logLevel: LogLevel, format: String, vararg args: Any) {
    xLog().log(logLevel.int, format, *args)
}

public sealed class LogLevel(public val int: Int, public val androidLevel: Int) {
    public object None : LogLevel(XLogLevel.NONE, Log.ASSERT)
    public object Error : LogLevel(XLogLevel.ERROR, Log.ERROR)
    public object Warn : LogLevel(XLogLevel.WARN, Log.WARN)
    public object Info : LogLevel(XLogLevel.INFO, Log.INFO)
    public object Debug : LogLevel(XLogLevel.DEBUG, Log.DEBUG)
    public object Verbose : LogLevel(XLogLevel.VERBOSE, Log.VERBOSE)
    public object All : LogLevel(XLogLevel.ALL, Log.VERBOSE)

    public val name: String get() = getLevelName(this)
    public val shortName: String get() = getLevelShortName(this)

    public companion object {
        public fun getLevelName(logLevel: LogLevel): String = XLogLevel.getLevelName(logLevel.int)
        public fun getLevelShortName(logLevel: LogLevel): String = XLogLevel.getShortLevelName(logLevel.int)

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

@Deprecated("Use proper throwable function", ReplaceWith("""xLogE("", log)"""))
public fun Any.xLogE(log: Throwable) {
    xLogStack().e(log)
}

@Deprecated("Use proper throwable function", ReplaceWith("""xLogW("", log)"""))
public fun Any.xLogW(log: Throwable) {
    xLogStack().w(log)
}

@Deprecated("Use proper throwable function", ReplaceWith("""xLogD("", log)"""))
public fun Any.xLogD(log: Throwable) {
    xLogStack().d(log)
}

@Deprecated("Use proper throwable function", ReplaceWith("""xLogI("", log)"""))
public fun Any.xLogI(log: Throwable) {
    xLogStack().i(log)
}

@Deprecated("Use proper throwable function", ReplaceWith("""xLog(logLevel, "", log)"""))
public fun Any.xLog(logLevel: LogLevel, log: Throwable) {
    xLogStack().log(logLevel.int, log)
}
