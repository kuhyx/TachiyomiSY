package eu.kanade.tachiyomi.network

/** Receives download progress from a response body. */
public interface ProgressListener {
    /** Called as bytes arrive; [contentLength] is -1 when unknown. */
    public fun update(bytesRead: Long, contentLength: Long, done: Boolean)
}
