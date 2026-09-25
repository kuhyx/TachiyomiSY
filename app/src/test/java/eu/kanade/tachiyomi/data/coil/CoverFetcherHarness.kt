package eu.kanade.tachiyomi.data.coil

import android.content.Context
import coil3.Extras
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.fetch.FetchResult
import coil3.fetch.SourceFetchResult
import coil3.request.CachePolicy
import coil3.request.Options
import eu.kanade.tachiyomi.source.online.HttpSource
import io.mockk.every
import io.mockk.mockk
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Path.Companion.toOkioPath
import java.io.File

internal fun coilOptions(
    context: Context = mockk(relaxed = true),
    disk: CachePolicy = CachePolicy.ENABLED,
    network: CachePolicy = CachePolicy.ENABLED,
    extras: Extras = Extras.EMPTY,
): Options = Options(context = context, diskCachePolicy = disk, networkCachePolicy = network, extras = extras)

internal fun realDiskCache(dir: File): DiskCache = DiskCache.Builder()
    .directory(dir.toOkioPath())
    .maxSizeBytes(1024L * 1024L)
    .build()

internal fun loaderWith(diskCache: DiskCache?): ImageLoader =
    mockk<ImageLoader>().also { every { it.diskCache } returns diskCache }

/** A key whose second and later reads throw: the one failure a fetcher's cleanup arms can observe. */
internal class FlakyKey(private val key: String = "key", private val goodReads: Int = 1) : Lazy<String> {
    private var reads = 0

    override val value: String
        get() = if (reads++ < goodReads) key else error("key gone")

    override fun isInitialized(): Boolean = true
}

/** A client that answers every call with [code] and [body], optionally as if it came from a cache. */
internal fun cannedClient(
    code: Int = 200,
    body: ResponseBody = "cover".toResponseBody(),
    fromCache: Boolean = false,
    onRequest: (okhttp3.Request) -> Unit = {},
): OkHttpClient = OkHttpClient.Builder()
    .addInterceptor { chain ->
        onRequest(chain.request())
        val builder = Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("canned $code")
            .body(body)
        if (fromCache) {
            builder.cacheResponse(
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(code)
                    .message("cached")
                    .build(),
            )
        }
        builder.build()
    }
    .build()

internal class CoverFetcherBuilder(
    var url: String? = "https://example.org/cover.jpg",
    var isLibrary: Boolean = false,
    var options: Options = coilOptions(),
    var coverFile: File? = null,
    var customCoverFile: File = File("/nonexistent/custom"),
    var diskCacheKey: Lazy<String> = lazyOf("key"),
    var source: HttpSource? = null,
    var client: Call.Factory = cannedClient(),
    var diskCache: DiskCache? = null,
) {
    fun build(): MangaCoverFetcher = MangaCoverFetcher(
        url = url,
        isLibraryManga = isLibrary,
        options = options,
        cover = MangaCoverFetcher.CoverLookups(
            coverFile = lazyOf(coverFile),
            customCoverFile = lazyOf(customCoverFile),
            diskCacheKey = diskCacheKey,
            source = lazyOf(source),
        ),
        callFactoryLazy = lazyOf(client),
        imageLoader = loaderWith(diskCache),
    )
}

internal fun coverFetcher(configure: CoverFetcherBuilder.() -> Unit = {}): MangaCoverFetcher =
    CoverFetcherBuilder().apply(configure).build()

internal fun FetchResult.sourceResult(): SourceFetchResult = this as SourceFetchResult

internal fun SourceFetchResult.text(): String = source.source().readUtf8()

internal fun imageResponse(body: ResponseBody = "cover".toResponseBody()): Response = Response.Builder()
    .request(okhttp3.Request.Builder().url("https://example.org/cover.jpg").build())
    .protocol(Protocol.HTTP_1_1)
    .code(200)
    .message("OK")
    .body(body)
    .build()

/** A directory that cannot be deleted (it has a child), so writing a file over it fails. */
internal fun stubbornDirectory(parent: File, name: String): File =
    File(parent, name).apply { mkdirs() }.also { File(it, "child").writeText("x") }
