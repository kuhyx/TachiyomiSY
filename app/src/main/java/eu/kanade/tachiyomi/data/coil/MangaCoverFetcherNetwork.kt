package eu.kanade.tachiyomi.data.coil

import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.disk.DiskCache
import coil3.fetch.FetchResult
import coil3.fetch.SourceFetchResult
import eu.kanade.tachiyomi.data.coil.MangaCoverFetcher.Companion.CACHE_CONTROL_NO_NETWORK_NO_CACHE
import eu.kanade.tachiyomi.data.coil.MangaCoverFetcher.Companion.CACHE_CONTROL_NO_STORE
import eu.kanade.tachiyomi.data.coil.MangaCoverFetcher.Companion.HTTP_NOT_MODIFIED
import eu.kanade.tachiyomi.network.await
import okhttp3.Request
import okhttp3.Response
import okio.FileSystem
import okio.source
import java.io.File
import java.io.IOException

private const val IMAGE = "image/*"

internal suspend fun MangaCoverFetcher.httpLoader(): FetchResult {
    // Only cache separately if it's a library item
    val libraryCoverCacheFile = if (isLibraryManga) {
        coverFileLazy.value ?: error("No cover specified")
    } else {
        null
    }
    if (libraryCoverCacheFile?.exists() == true && options.diskCachePolicy.readEnabled) {
        return fileLoader(libraryCoverCacheFile)
    }

    val snapshot = readFromDiskCache()
    return try {
        if (snapshot != null) fromSnapshot(snapshot, libraryCoverCacheFile) else fromNetwork(libraryCoverCacheFile)
    } catch (expected: Exception) {
        // Rethrown (or wrapped) whatever the cause.
        snapshot?.close()
        throw expected
    }
}

// Fetch from disk cache
internal fun MangaCoverFetcher.fromSnapshot(snapshot: DiskCache.Snapshot, libraryCoverCacheFile: File?): FetchResult {
    val snapshotCoverCache = moveSnapshotToCoverCache(snapshot, libraryCoverCacheFile)
    if (snapshotCoverCache != null) {
        // Read from cover cache after added to library
        return fileLoader(snapshotCoverCache)
    }

    // Read from snapshot
    return SourceFetchResult(
        source = snapshot.toImageSource(),
        mimeType = IMAGE,
        dataSource = DataSource.DISK,
    )
}

// Fetch from network; whatever was opened is closed again on failure.
internal suspend fun MangaCoverFetcher.fromNetwork(libraryCoverCacheFile: File?): FetchResult {
    val response = executeNetworkRequest()
    val responseBody = response.body
    var snapshot: DiskCache.Snapshot? = null
    try {
        // Read from cover cache after library manga cover updated
        val responseCoverCache = writeResponseToCoverCache(response, libraryCoverCacheFile)
        if (responseCoverCache != null) {
            return fileLoader(responseCoverCache)
        }

        // Read from disk cache, else from the response if cache is unused or unusable
        snapshot = writeToDiskCache(response)
        return if (snapshot != null) {
            SourceFetchResult(
                source = snapshot.toImageSource(),
                mimeType = IMAGE,
                dataSource = DataSource.NETWORK,
            )
        } else {
            SourceFetchResult(
                source = ImageSource(source = responseBody.source(), fileSystem = FileSystem.SYSTEM),
                mimeType = IMAGE,
                dataSource = if (response.cacheResponse != null) DataSource.DISK else DataSource.NETWORK,
            )
        }
    } catch (expected: Exception) {
        // Rethrown (or wrapped) whatever the cause.
        responseBody.close()
        snapshot?.close()
        throw expected
    }
}

internal suspend fun MangaCoverFetcher.executeNetworkRequest(): Response {
    val client = sourceLazy.value?.client ?: callFactoryLazy.value
    val response = client.newCall(newRequest()).await()
    if (!response.isSuccessful && response.code != HTTP_NOT_MODIFIED) {
        response.close()
        throw IOException(response.message)
    }
    return response
}

internal fun MangaCoverFetcher.newRequest(): Request {
    val request = Request.Builder().apply {
        url(this@newRequest.url!!)

        val sourceHeaders = sourceLazy.value?.headers
        if (sourceHeaders != null) {
            headers(sourceHeaders)
        }
    }

    if (options.networkCachePolicy.readEnabled) {
        // don't take up okhttp cache
        request.cacheControl(CACHE_CONTROL_NO_STORE)
    } else {
        // This causes the request to fail with a 504 Unsatisfiable Request.
        request.cacheControl(CACHE_CONTROL_NO_NETWORK_NO_CACHE)
    }

    return request.build()
}
