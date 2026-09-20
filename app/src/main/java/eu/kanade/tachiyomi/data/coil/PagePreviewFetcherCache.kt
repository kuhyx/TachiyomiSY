package eu.kanade.tachiyomi.data.coil

import coil3.disk.DiskCache
import logcat.LogPriority
import okhttp3.Response
import okio.Source
import tachiyomi.core.common.util.system.logcat
import java.io.File

internal fun PagePreviewFetcher.moveSnapshotToPagePreviewCache(snapshot: DiskCache.Snapshot): File? {
    return try {
        imageLoader.diskCache?.run {
            fileSystem.source(snapshot.data).use { input ->
                writeSourceToPagePreviewCache(input)
            }
            remove(diskCacheKey)
        }
        return if (isInCache()) {
            pagePreviewFile()
        } else {
            null
        }
    } catch (expected: Exception) {
        // Logged whatever the cause; the caller carries on.
        logcat(LogPriority.ERROR, expected) { "Failed to write snapshot data to page preview cache $diskCacheKey" }
        null
    }
}

internal fun PagePreviewFetcher.writeResponseToPreviewCache(response: Response): File? {
    if (!options.diskCachePolicy.writeEnabled) return null
    return try {
        response.peekBody(Long.MAX_VALUE).source().use { input ->
            writeSourceToPagePreviewCache(input)
        }
        return if (isInCache()) {
            pagePreviewFile()
        } else {
            null
        }
    } catch (expected: Exception) {
        // Logged whatever the cause; the caller carries on.
        logcat(LogPriority.ERROR, expected) { "Failed to write response data to page preview cache $diskCacheKey" }
        null
    }
}

internal fun PagePreviewFetcher.writeSourceToPagePreviewCache(input: Source) {
    writeToCache(input)
}

internal fun PagePreviewFetcher.readFromDiskCache(): DiskCache.Snapshot? =
    if (options.diskCachePolicy.readEnabled) imageLoader.diskCache?.openSnapshot(diskCacheKey) else null

internal fun PagePreviewFetcher.writeToDiskCache(
    response: Response,
): DiskCache.Snapshot? {
    val diskCache = imageLoader.diskCache
    val editor = diskCache?.openEditor(diskCacheKey) ?: return null
    try {
        diskCache.fileSystem.write(editor.data) {
            response.body.source().readAll(this)
        }
        return editor.commitAndOpenSnapshot()
    } catch (expected: Exception) {
        // Rethrown (or wrapped) whatever the cause.
        try {
            editor.abort()
        } catch (ignored: Exception) {
        }
        throw expected
    }
}
