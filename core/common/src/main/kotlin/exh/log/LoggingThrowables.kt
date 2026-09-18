package exh.log

// Throwable-carrying loggers of the xLog family.

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
