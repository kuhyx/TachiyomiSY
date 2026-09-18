package exh.log

// String-message loggers of the xLog family, one file per overload set so detekt's per-file caps hold.

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
