package eu.kanade.tachiyomi

import android.content.Context
import coil3.ImageLoader
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.allowRgb565
import coil3.request.crossfade
import coil3.util.DebugLogger
import eu.kanade.tachiyomi.data.coil.BufferedSourceFetcher
import eu.kanade.tachiyomi.data.coil.MangaCoverFetcher
import eu.kanade.tachiyomi.data.coil.MangaCoverKeyer
import eu.kanade.tachiyomi.data.coil.MangaKeyer
import eu.kanade.tachiyomi.data.coil.PagePreviewFetcher
import eu.kanade.tachiyomi.data.coil.PagePreviewKeyer
import eu.kanade.tachiyomi.data.coil.TachiyomiImageDecoder
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.util.system.DeviceUtil
import eu.kanade.tachiyomi.util.system.animatorDurationScale
import kotlinx.coroutines.Dispatchers
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

private const val CROSSFADE_MS = 300
private const val IMAGE_FETCH_THREADS = 8
private const val IMAGE_DECODE_THREADS = 3

internal fun App.buildImageLoader(context: Context): ImageLoader {
    return ImageLoader.Builder(this).apply {
        val callFactoryLazy = lazy { Injekt.get<NetworkHelper>().client }
        components {
            // NetworkFetcher.Factory
            add(OkHttpNetworkFetcherFactory(callFactoryLazy::value))
            // Decoder.Factory
            add(TachiyomiImageDecoder.Factory())
            // Fetcher.Factory
            add(BufferedSourceFetcher.Factory())
            add(MangaCoverFetcher.MangaCoverFactory(callFactoryLazy))
            add(MangaCoverFetcher.MangaFactory(callFactoryLazy))
            // SY -->
            add(PagePreviewFetcher.Factory(callFactoryLazy))
            // SY <--
            // Keyer
            add(MangaCoverKeyer())
            add(MangaKeyer())
            // SY -->
            add(PagePreviewKeyer())
            // SY <--
        }

        memoryCache(
            MemoryCache.Builder()
                .maxSizePercent(context)
                .build(),
        )

        crossfade((CROSSFADE_MS * this@buildImageLoader.animatorDurationScale).toInt())
        allowRgb565(DeviceUtil.isLowRamDevice(this@buildImageLoader))
        if (networkPreferences.verboseLogging.get()) logger(DebugLogger())

        // Coil spawns a new thread for every image load by default
        fetcherCoroutineContext(Dispatchers.IO.limitedParallelism(IMAGE_FETCH_THREADS))
        decoderCoroutineContext(Dispatchers.IO.limitedParallelism(IMAGE_DECODE_THREADS))
    }
        .build()
}
