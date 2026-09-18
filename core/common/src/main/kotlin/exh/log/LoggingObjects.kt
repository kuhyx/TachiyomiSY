package exh.log

// Object and format-string loggers of the xLog family.

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
