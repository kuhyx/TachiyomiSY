package eu.kanade.tachiyomi.util.lang

import java.io.Closeable

/**
 * Executes the given block function on this resources and then closes it down correctly whether an exception is
 * thrown or not.
 *
 * @param T the closeable element type.
 * @param block a function to process with given Closeable resources.
 * @return the result of block function invoked on this resource.
 */
internal inline fun <T : Closeable?> Array<T>.use(block: () -> Unit) {
    var blockException: Throwable? = null
    try {
        return block()
    } catch (expected: Throwable) {
        // Rethrown (or wrapped) whatever the cause.
        blockException = expected
        throw expected
    } finally {
        closeAll(blockException)
    }
}

// Closes every element; once the block has failed, a failing close is attached to that failure instead of thrown.
internal fun <T : Closeable?> Array<T>.closeAll(blockException: Throwable?) {
    forEach {
        try {
            it?.close()
        } catch (expected: Throwable) {
            if (blockException == null) throw expected
            blockException.addSuppressed(expected)
        }
    }
}
