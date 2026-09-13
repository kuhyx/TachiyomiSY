package eu.kanade.tachiyomi.network

public interface ProgressListener {
    public fun update(bytesRead: Long, contentLength: Long, done: Boolean)
}
