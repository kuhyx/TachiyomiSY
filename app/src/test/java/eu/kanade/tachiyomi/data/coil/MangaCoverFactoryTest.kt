package eu.kanade.tachiyomi.data.coil

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import coil3.ComponentRegistry
import coil3.ImageLoader
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.online.HttpSource
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import tachiyomi.domain.manga.interactor.GetCustomMangaInfo
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaCover
import tachiyomi.domain.source.service.SourceManager
import java.io.ByteArrayInputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
internal class MangaCoverFactoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val coverCache: CoverCache = mockk()
    private val sourceManager: SourceManager = mockk()
    private val httpSource: HttpSource = mockk()
    private val customInfo: GetCustomMangaInfo = mockk()
    private val loader: ImageLoader = mockk<ImageLoader>().also {
        val registry = ComponentRegistry.Builder()
            .add(MangaKeyer(), Manga::class)
            .add(MangaCoverKeyer(coverCache), MangaCover::class)
            .build()
        every { it.components } returns registry
    }

    @Before
    fun setUp() {
        every { coverCache.getCoverFile(any()) } returns File("/covers/a")
        every { coverCache.getCustomCoverFile(any()) } returns File("/covers/custom")
        every { customInfo.get(any()) } returns null
        every { sourceManager.get(1L) } returns httpSource
        every { sourceManager.get(2L) } returns mockk<Source>()
        startKoin {
            modules(
                module {
                    single { coverCache }
                    single { sourceManager }
                    single { customInfo }
                },
            )
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun mangaFactoryWiresTheLookups() {
        val manga = Manga.create().copy(id = 4L, source = 1L, ogThumbnailUrl = "https://x/c.jpg", favorite = true)
        val fetcher = MangaCoverFetcher.MangaFactory(lazyOf(OkHttpClient()))
            .create(data = manga, options = coilOptions(), imageLoader = loader) as MangaCoverFetcher
        fetcher.url shouldBe "https://x/c.jpg"
        fetcher.isLibraryManga shouldBe true
        fetcher.coverFileLazy.value shouldBe File("/covers/a")
        fetcher.sourceLazy.value shouldBe httpSource
        fetcher.diskCacheKey shouldBe "https://x/c.jpg;0"
    }

    @Test
    fun coverFactoryWiresTheLookups() {
        val cover = MangaCover(mangaId = 4L, sourceId = 2L, isMangaFavorite = false, ogUrl = "u", lastModified = 3L)
        val fetcher = MangaCoverFetcher.MangaCoverFactory(lazyOf(OkHttpClient()))
            .create(data = cover, options = coilOptions(), imageLoader = loader) as MangaCoverFetcher
        fetcher.url shouldBe "u"
        fetcher.isLibraryManga shouldBe false
        fetcher.coverFileLazy.value shouldBe File("/covers/a")
        fetcher.sourceLazy.value.shouldBeNull()
        fetcher.diskCacheKey shouldBe "u;3"
        val httpCover = MangaCoverFetcher.MangaCoverFactory(lazyOf(OkHttpClient()))
            .create(data = cover.copy(sourceId = 1L), options = coilOptions(), imageLoader = loader)
        (httpCover as MangaCoverFetcher).sourceLazy.value shouldBe httpSource
    }

    @Test
    fun customCoverComesFromTheCache() = runTest {
        val custom = File(context.cacheDir, "custom-cover").apply { writeText("custom") }
        every { coverCache.getCustomCoverFile(4L) } returns custom
        every { sourceManager.get(3L) } returns null
        val manga = Manga.create().copy(id = 4L, source = 3L)
        val mangaFetcher = MangaCoverFetcher.MangaFactory(lazyOf(OkHttpClient()))
            .create(data = manga, options = coilOptions(), imageLoader = loader) as MangaCoverFetcher
        mangaFetcher.sourceLazy.value.shouldBeNull()
        mangaFetcher.fetch().sourceResult().text() shouldBe "custom"
        val cover = MangaCover(mangaId = 4L, sourceId = 3L, isMangaFavorite = true, ogUrl = null, lastModified = 0L)
        MangaCoverFetcher.MangaCoverFactory(lazyOf(OkHttpClient()))
            .create(data = cover, options = coilOptions(), imageLoader = loader)
            .fetch()!!
            .sourceResult()
            .text() shouldBe "custom"
    }

    @Test
    fun contentUriIsReadFromResolver() = runTest {
        val uri = Uri.parse("content://covers/1")
        Shadows.shadowOf(context.contentResolver).registerInputStream(uri, ByteArrayInputStream("uri".toByteArray()))
        val fetcher = coverFetcher {
            url = uri.toString()
            options = coilOptions(context = context)
        }
        fetcher.fetch().sourceResult().text() shouldBe "uri"
    }
}
