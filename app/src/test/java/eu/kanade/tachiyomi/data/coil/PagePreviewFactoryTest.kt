package eu.kanade.tachiyomi.data.coil

import coil3.ComponentRegistry
import coil3.ImageLoader
import coil3.disk.DiskCache
import eu.kanade.domain.manga.model.PagePreview
import eu.kanade.tachiyomi.data.cache.PagePreviewCache
import eu.kanade.tachiyomi.source.PagePreviewSource
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okio.Buffer
import okio.FileSystem
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.domain.source.service.SourceManager
import java.io.File
import java.io.IOException

internal class PagePreviewFactoryTest {

    @TempDir
    lateinit var dir: File

    private val cache: PagePreviewCache = mockk(relaxed = true)
    private val sourceManager: SourceManager = mockk()
    private val preview = PagePreview(index = 1, imageUrl = "https://example.org/1.jpg", source = 7L)

    @BeforeEach
    fun setUp() {
        startKoin {
            modules(
                module {
                    single { cache }
                    single { sourceManager }
                },
            )
        }
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    private fun create(): PagePreviewFetcher {
        val loader = mockk<ImageLoader>()
        val registry = ComponentRegistry.Builder().add(PagePreviewKeyer(), PagePreview::class).build()
        every { loader.components } returns registry
        every { loader.diskCache } returns null
        return PagePreviewFetcher.Factory(lazyOf(cannedClient()))
            .create(data = preview, options = coilOptions(), imageLoader = loader) as PagePreviewFetcher
    }

    @Test
    fun cacheCallbacksUseTheImageUrl() = runTest {
        val file = File(dir, "p").apply { writeText("cached") }
        every { cache.getImageFile(preview.imageUrl) } returns file
        every { cache.isImageInCache(preview.imageUrl) } returns true
        val fetcher = create()
        fetcher.fetch().sourceResult().text() shouldBe "cached"
        fetcher.diskCacheKey shouldBe preview.imageUrl
        val input = Buffer()
        fetcher.writeToCache(input)
        verify { cache.putImageToCache(preview.imageUrl, input) }
    }

    @Test
    fun previewSourceIsLookedUp() = runTest {
        val source = mockk<PagePreviewSource>()
        every { source.id } returns 7L
        every { sourceManager.get(7L) } returns source
        every { cache.isImageInCache(any()) } returns false
        coEvery { source.fetchPreviewImage(any(), any()) } returns imageResponse()
        create().fetch().sourceResult().text() shouldBe "cover"
        every { sourceManager.get(7L) } returns null
        create().fetch().sourceResult().text() shouldBe "cover"
    }

    @Test
    fun failedAbortIsIgnored() {
        val editor = mockk<DiskCache.Editor>()
        every { editor.data } throws IOException("gone")
        every { editor.abort() } throws IOException("also gone")
        val disk = mockk<DiskCache>()
        every { disk.openEditor(any()) } returns editor
        every { disk.fileSystem } returns FileSystem.SYSTEM
        val fetcher = PagePreviewFetcher(
            page = preview,
            options = coilOptions(),
            pagePreviewFile = { File(dir, "p") },
            isInCache = { false },
            writeToCache = {},
            diskCacheKeyLazy = lazyOf("key"),
            sourceLazy = lazyOf(null),
            callFactoryLazy = lazyOf(OkHttpClient()),
            imageLoader = loaderWith(disk),
        )
        shouldThrow<IOException> { fetcher.writeToDiskCache(imageResponse()) }
    }
}
