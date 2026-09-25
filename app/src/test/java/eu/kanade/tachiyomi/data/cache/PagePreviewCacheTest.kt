package eu.kanade.tachiyomi.data.cache

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import eu.kanade.tachiyomi.source.PagePreviewInfo
import eu.kanade.tachiyomi.source.PagePreviewPage
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import okio.Buffer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.manga.model.Manga
import java.io.File

@RunWith(RobolectricTestRunner::class)
internal class PagePreviewCacheTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val manga = Manga.create().copy(id = 5L)
    private val page = PagePreviewPage(
        page = 2,
        pagePreviews = listOf(PagePreviewInfo(index = 0, imageUrl = "https://example.org/0.jpg")),
        hasNextPage = true,
        pagePreviewPages = 4,
    )
    private lateinit var cache: PagePreviewCache

    @Before
    fun setUp() {
        File(context.cacheDir, PagePreviewCache.PARAMETER_CACHE_DIRECTORY).deleteRecursively()
        startKoin { modules(module { single<Json> { Json } }) }
        cache = PagePreviewCache(context)
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun pageListRoundTrips() {
        cache.putPageListToCache(manga = manga, chapterIds = listOf(1L, 2L), pages = page)
        val cached = cache.getPageListFromCache(manga = manga, chapterIds = listOf(1L, 2L), page = 2)
        cached.copy(pagePreviews = emptyList()) shouldBe page.copy(pagePreviews = emptyList())
        cached.pagePreviews.map { it.imageUrl } shouldBe listOf("https://example.org/0.jpg")
    }

    @Test
    fun imageRoundTrips() {
        cache.isImageInCache("https://example.org/0.jpg") shouldBe false
        cache.putImageToCache("https://example.org/0.jpg", Buffer().writeUtf8("jpeg"))
        cache.isImageInCache("https://example.org/0.jpg") shouldBe true
        cache.getImageFile("https://example.org/0.jpg").readText() shouldBe "jpeg"
    }

    @Test
    fun clearKeepsTheJournal() {
        cache.putImageToCache("https://example.org/0.jpg", Buffer().writeUtf8("a"))
        cache.putImageToCache("https://example.org/1.jpg", Buffer().writeUtf8("b"))
        cache.readableSize.isNotEmpty() shouldBe true
        cache.clear() shouldBe 2
    }
}
