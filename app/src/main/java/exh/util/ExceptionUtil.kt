package exh.util

internal inline fun <T> ignore(expr: () -> T): T? {
    return try {
        expr()
    } catch (_: Throwable) {
        // Any failure ends here and the fallback below applies.
        null
    }
}

internal fun <T : Throwable> T.withRootCause(cause: Throwable): T {
    val curCause = this.cause

    if (curCause == null) {
        this.initCause(cause)
    } else {
        curCause.withRootCause(cause)
    }

    return this
}
