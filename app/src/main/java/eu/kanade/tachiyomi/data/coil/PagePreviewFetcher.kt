package eu.kanade.tachiyomi.data.coil

import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.disk.DiskCache
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import eu.kanade.domain.manga.model.PagePreview
import eu.kanade.tachiyomi.data.cache.PagePreviewCache
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.source.PagePreviewSource
import eu.kanade.tachiyomi.source.online.HttpSource
import exh.source.getMainSource
import okhttp3.CacheControl
import okhttp3.Call
import okhttp3.Request
import okhttp3.Response
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import okio.Source
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.injectLazy
import java.io.File
import java.io.IOException

private const val IMAGE = "image/*"

/**
 * A [Fetcher] that fetches page preview image for [PagePreview] object.
 *
 * Disk caching is handled by [PagePreviewCache], otherwise
 * handled by Coil's [DiskCache].
 */
@Suppress("LongParameterList")
internal class PagePreviewFetcher(
    private val page: PagePreview,
    internal val options: Options,
    internal val pagePreviewFile: () -> File,
    internal val isInCache: () -> Boolean,
    internal val writeToCache: (Source) -> Unit,
    private val diskCacheKeyLazy: Lazy<String>,
    private val sourceLazy: Lazy<PagePreviewSource?>,
    private val callFactoryLazy: Lazy<Call.Factory>,
    internal val imageLoader: ImageLoader,
) : Fetcher {

    internal val diskCacheKey: String
        get() = diskCacheKeyLazy.value

    override suspend fun fetch(): FetchResult = httpLoader()

    private fun fileLoader(file: File): FetchResult {
        return SourceFetchResult(
            source = ImageSource(
                file = file.toOkioPath(),
                fileSystem = FileSystem.SYSTEM,
                diskCacheKey = diskCacheKey,
            ),
            mimeType = IMAGE,
            dataSource = DataSource.DISK,
        )
    }

    private suspend fun httpLoader(): FetchResult {
        if (isInCache() && options.diskCachePolicy.readEnabled) {
            return fileLoader(pagePreviewFile())
        }
        val snapshot = readFromDiskCache()
        return try {
            if (snapshot != null) fromSnapshot(snapshot) else fromNetwork()
        } catch (expected: Exception) {
            // Rethrown (or wrapped) whatever the cause.
            snapshot?.close()
            throw expected
        }
    }

    // Fetch from disk cache
    private fun fromSnapshot(snapshot: DiskCache.Snapshot): FetchResult {
        val snapshotPagePreviewCache = moveSnapshotToPagePreviewCache(snapshot)
        if (snapshotPagePreviewCache != null) {
            // Read from page preview cache
            return fileLoader(snapshotPagePreviewCache)
        }

        // Read from snapshot
        return SourceFetchResult(
            source = snapshot.toImageSource(),
            mimeType = IMAGE,
            dataSource = DataSource.DISK,
        )
    }

    // Fetch from network; whatever was opened is closed again on failure.
    private suspend fun fromNetwork(): FetchResult {
        val response = executeNetworkRequest()
        val responseBody = checkNotNull(response.body) { "Null response source" }
        var snapshot: DiskCache.Snapshot? = null
        try {
            // Read from page preview cache after page preview updated
            val responsePagePreviewCache = writeResponseToPreviewCache(response)
            if (responsePagePreviewCache != null) {
                return fileLoader(responsePagePreviewCache)
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

    private suspend fun executeNetworkRequest(): Response {
        val response = sourceLazy.value?.fetchPreviewImage(
            page.toPagePreviewInfo(), getCacheControl(),
        ) ?: callFactoryLazy.value.newCall(newRequest()).await()
        if (!response.isSuccessful && response.code != HTTP_NOT_MODIFIED) {
            response.close()
            throw IOException(response.message)
        }
        return response
    }

    private fun getCacheControl(): CacheControl {
        return if (options.networkCachePolicy.readEnabled) {
            // don't take up okhttp cache
            CACHE_CONTROL_NO_STORE
        } else {
            // This causes the request to fail with a 504 Unsatisfiable Request.
            CACHE_CONTROL_NO_NETWORK_NO_CACHE
        }
    }

    private fun newRequest(): Request {
        val request = Request.Builder().apply {
            url(page.imageUrl)

            val sourceHeaders = (sourceLazy.value as? HttpSource)?.headers
            if (sourceHeaders != null) {
                headers(sourceHeaders)
            }
        }

        request.cacheControl(getCacheControl())

        return request.build()
    }

    private fun DiskCache.Snapshot.toImageSource(): ImageSource {
        return ImageSource(
            file = data,
            fileSystem = FileSystem.SYSTEM,
            diskCacheKey = diskCacheKey,
            closeable = this,
        )
    }

    class Factory(
        private val callFactoryLazy: Lazy<Call.Factory>,
    ) : Fetcher.Factory<PagePreview> {

        private val pagePreviewCache: PagePreviewCache by injectLazy()
        private val sourceManager: SourceManager by injectLazy()

        override fun create(data: PagePreview, options: Options, imageLoader: ImageLoader): Fetcher {
            return PagePreviewFetcher(
                page = data,
                options = options,
                pagePreviewFile = { pagePreviewCache.getImageFile(data.imageUrl) },
                isInCache = { pagePreviewCache.isImageInCache(data.imageUrl) },
                writeToCache = { pagePreviewCache.putImageToCache(data.imageUrl, it) },
                diskCacheKeyLazy = lazy { imageLoader.components.key(data, options)!! },
                sourceLazy = lazy { sourceManager.get(data.source)?.getMainSource<PagePreviewSource>() },
                callFactoryLazy = callFactoryLazy,
                imageLoader = imageLoader,
            )
        }
    }

    companion object {
        private val CACHE_CONTROL_NO_STORE = CacheControl.Builder().noStore().build()
        private val CACHE_CONTROL_NO_NETWORK_NO_CACHE = CacheControl.Builder().noCache().onlyIfCached().build()

        private const val HTTP_NOT_MODIFIED = 304
    }
}
