package eu.kanade.tachiyomi.data.coil

import coil3.disk.DiskCache
import logcat.LogPriority
import okhttp3.Response
import okio.Source
import okio.buffer
import okio.sink
import okio.source
import tachiyomi.core.common.util.system.logcat
import java.io.File

internal fun MangaCoverFetcher.moveSnapshotToCoverCache(snapshot: DiskCache.Snapshot, cacheFile: File?): File? {
    if (cacheFile == null) return null
    return try {
        imageLoader.diskCache?.run {
            fileSystem.source(snapshot.data).use { input ->
                writeSourceToCoverCache(input, cacheFile)
            }
            remove(diskCacheKey)
        }
        cacheFile.takeIf { it.exists() }
    } catch (expected: Exception) {
        // Logged whatever the cause; the caller carries on.
        logcat(LogPriority.ERROR, expected) { "Failed to write snapshot data to cover cache ${cacheFile.name}" }
        null
    }
}

internal fun MangaCoverFetcher.writeResponseToCoverCache(response: Response, cacheFile: File?): File? {
    if (cacheFile == null || !options.diskCachePolicy.writeEnabled) return null
    return try {
        response.peekBody(Long.MAX_VALUE).source().use { input ->
            writeSourceToCoverCache(input, cacheFile)
        }
        // writeSourceToCoverCache either leaves the file written or throws.
        cacheFile
    } catch (expected: Exception) {
        // Logged whatever the cause; the caller carries on.
        logcat(LogPriority.ERROR, expected) { "Failed to write response data to cover cache ${cacheFile.name}" }
        null
    }
}

internal fun MangaCoverFetcher.writeSourceToCoverCache(input: Source, cacheFile: File) {
    cacheFile.parentFile?.mkdirs()
    cacheFile.delete()
    try {
        cacheFile.sink().buffer().use { output ->
            output.writeAll(input)
        }
    } catch (expected: Exception) {
        // Rethrown (or wrapped) whatever the cause.
        cacheFile.delete()
        throw expected
    }
}

internal fun MangaCoverFetcher.readFromDiskCache(): DiskCache.Snapshot? {
    return if (options.diskCachePolicy.readEnabled) {
        imageLoader.diskCache?.openSnapshot(diskCacheKey)
    } else {
        null
    }
}

internal fun MangaCoverFetcher.writeToDiskCache(
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
