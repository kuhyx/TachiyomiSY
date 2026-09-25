package eu.kanade.tachiyomi.data.cache

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.jakewharton.disklrucache.DiskLruCache
import eu.kanade.tachiyomi.source.PagePreviewPage
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
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
import java.io.IOException

/** The failure arms, over a stubbed [DiskLruCache] a real one cannot be pushed into. */
@RunWith(RobolectricTestRunner::class)
internal class PagePreviewCacheFailureTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val disk: DiskLruCache = mockk(relaxed = true)
    private val dir: File = File(context.cacheDir, "stub_preview_cache").apply { mkdirs() }
    private val page = PagePreviewPage(page = 1, pagePreviews = emptyList(), hasNextPage = false, pagePreviewPages = null)
    private lateinit var cache: PagePreviewCache

    @Before
    fun setUp() {
        mockkStatic(DiskLruCache::class)
        every { DiskLruCache.open(any(), any(), any(), any()) } returns disk
        every { disk.directory } returns dir
        startKoin { modules(module { single<Json> { Json } }) }
        cache = PagePreviewCache(context)
    }

    @After
    fun tearDown() {
        stopKoin()
        unmockkAll()
        dir.deleteRecursively()
    }

    @Test
    fun busyEntryIsSkipped() {
        every { disk.edit(any()) } returns null
        cache.putPageListToCache(manga = Manga.create(), chapterIds = emptyList(), pages = page)
        shouldThrow<IOException> { cache.putImageToCache("https://example.org/p.jpg", Buffer()) }
        verify(exactly = 0) { disk.flush() }
    }

    @Test
    fun failedWritesAbortTheEditor() {
        val editor = mockk<DiskLruCache.Editor>(relaxed = true)
        every { disk.edit(any()) } returns editor
        every { editor.newOutputStream(0) } throws IOException("disk full")
        cache.putPageListToCache(manga = Manga.create(), chapterIds = emptyList(), pages = page)
        shouldThrow<IOException> { cache.putImageToCache("https://example.org/p.jpg", Buffer()) }
        verify(exactly = 2) { editor.abortUnlessCommitted() }
    }

    @Test
    fun failedEditIsHandled() {
        every { disk.edit(any()) } throws IOException("journal")
        cache.putPageListToCache(manga = Manga.create(), chapterIds = emptyList(), pages = page)
        shouldThrow<IOException> { cache.putImageToCache("https://example.org/p.jpg", Buffer()) }
    }

    @Test
    fun unreadableJournalIsNotCached() {
        every { disk.get(any()) } throws IOException("journal")
        cache.isImageInCache("https://example.org/p.jpg") shouldBe false
    }

    @Test
    fun failedRemovalIsNotCounted() {
        File(dir, "journal").writeText("")
        File(dir, "journal.bkp").writeText("")
        File(dir, "abc.0").writeText("")
        every { disk.remove("abc") } throws IOException("locked")
        cache.clear() shouldBe 0
        dir.deleteRecursively()
        cache.clear() shouldBe 0
    }
}
