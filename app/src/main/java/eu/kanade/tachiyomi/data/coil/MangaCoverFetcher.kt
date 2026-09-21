package eu.kanade.tachiyomi.data.coil

import androidx.core.net.toUri
import coil3.Extras
import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.disk.DiskCache
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.getOrDefault
import coil3.request.Options
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.coil.MangaCoverFetcher.Companion.USE_CUSTOM_COVER_KEY
import eu.kanade.tachiyomi.source.online.HttpSource
import okhttp3.CacheControl
import okhttp3.Call
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import okio.buffer
import okio.source
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaCover
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.injectLazy
import java.io.File

private const val IMAGE = "image/*"

/**
 * A [Fetcher] that fetches cover image for [Manga] object.
 *
 * It uses [Manga.thumbnailUrl] if custom cover is not set by the user.
 * Disk caching for library items is handled by [CoverCache], otherwise
 * handled by Coil's [DiskCache].
 *
 * Available request parameter:
 * - [USE_CUSTOM_COVER_KEY]: Use custom cover if set by user, default is true
 */
internal class MangaCoverFetcher(
    internal val url: String?,
    internal val isLibraryManga: Boolean,
    internal val options: Options,
    cover: CoverLookups,
    internal val callFactoryLazy: Lazy<Call.Factory>,
    internal val imageLoader: ImageLoader,
) : Fetcher {
    internal val coverFileLazy = cover.coverFile
    private val customCoverFileLazy = cover.customCoverFile
    private val diskCacheKeyLazy = cover.diskCacheKey
    internal val sourceLazy = cover.source

    /** Where this cover may come from, each resolved only when the fetch gets that far. */
    data class CoverLookups(
        val coverFile: Lazy<File?>,
        val customCoverFile: Lazy<File>,
        val diskCacheKey: Lazy<String>,
        val source: Lazy<HttpSource?>,
    )

    internal val diskCacheKey: String
        get() = diskCacheKeyLazy.value

    override suspend fun fetch(): FetchResult {
        // Use custom cover if exists
        val useCustomCover = options.extras.getOrDefault(USE_CUSTOM_COVER_KEY)
        if (useCustomCover) {
            val customCoverFile = customCoverFileLazy.value
            if (customCoverFile.exists()) {
                return fileLoader(customCoverFile)
            }
        }

        // diskCacheKey is thumbnail_url
        if (url == null) error("No cover specified")
        return when (getResourceType(url)) {
            Type.File -> fileLoader(File(url.substringAfter("file://")))
            Type.URI -> fileUriLoader(url)
            Type.URL -> httpLoader()
            null -> error("Invalid image")
        }
    }

    internal fun fileLoader(file: File): FetchResult {
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

    private fun fileUriLoader(uri: String): FetchResult {
        val source = UniFile.fromUri(options.context, uri.toUri())!!
            .openInputStream()
            .source()
            .buffer()
        return SourceFetchResult(
            source = ImageSource(source = source, fileSystem = FileSystem.SYSTEM),
            mimeType = IMAGE,
            dataSource = DataSource.DISK,
        )
    }

    internal fun DiskCache.Snapshot.toImageSource(): ImageSource {
        return ImageSource(
            file = data,
            fileSystem = FileSystem.SYSTEM,
            diskCacheKey = diskCacheKey,
            closeable = this,
        )
    }

    private fun getResourceType(cover: String?): Type? {
        return when {
            cover.isNullOrEmpty() -> null
            cover.startsWith("http", true) || cover.startsWith("Custom-", true) -> Type.URL
            cover.startsWith("/") || cover.startsWith("file://") -> Type.File
            cover.startsWith("content") -> Type.URI
            else -> null
        }
    }

    private enum class Type {
        File,
        URI,
        URL,
    }

    class MangaFactory(
        private val callFactoryLazy: Lazy<Call.Factory>,
    ) : Fetcher.Factory<Manga> {

        private val coverCache: CoverCache by injectLazy()
        private val sourceManager: SourceManager by injectLazy()

        override fun create(data: Manga, options: Options, imageLoader: ImageLoader): Fetcher {
            return MangaCoverFetcher(
                url = data.thumbnailUrl,
                isLibraryManga = data.favorite,
                options = options,
                cover = CoverLookups(
                    coverFile = lazy { coverCache.getCoverFile(data.thumbnailUrl) },
                    customCoverFile = lazy { coverCache.getCustomCoverFile(data.id) },
                    diskCacheKey = lazy { imageLoader.components.key(data, options)!! },
                    source = lazy { sourceManager.get(data.source) as? HttpSource },
                ),
                callFactoryLazy = callFactoryLazy,
                imageLoader = imageLoader,
            )
        }
    }

    class MangaCoverFactory(
        private val callFactoryLazy: Lazy<Call.Factory>,
    ) : Fetcher.Factory<MangaCover> {

        private val coverCache: CoverCache by injectLazy()
        private val sourceManager: SourceManager by injectLazy()

        override fun create(data: MangaCover, options: Options, imageLoader: ImageLoader): Fetcher {
            return MangaCoverFetcher(
                url = data.url,
                isLibraryManga = data.isMangaFavorite,
                options = options,
                cover = CoverLookups(
                    coverFile = lazy { coverCache.getCoverFile(data.url) },
                    customCoverFile = lazy { coverCache.getCustomCoverFile(data.mangaId) },
                    diskCacheKey = lazy { imageLoader.components.key(data, options)!! },
                    source = lazy { sourceManager.get(data.sourceId) as? HttpSource },
                ),
                callFactoryLazy = callFactoryLazy,
                imageLoader = imageLoader,
            )
        }
    }

    companion object {
        val USE_CUSTOM_COVER_KEY = Extras.Key(true)

        internal val CACHE_CONTROL_NO_STORE = CacheControl.Builder().noStore().build()
        internal val CACHE_CONTROL_NO_NETWORK_NO_CACHE = CacheControl.Builder().noCache().onlyIfCached().build()

        internal const val HTTP_NOT_MODIFIED = 304
    }
}
