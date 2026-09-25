package eu.kanade.tachiyomi.data.cache

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.jakewharton.disklrucache.DiskLruCache
import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.chapter.model.Chapter
import java.io.File
import java.io.IOException

/** The failure arms, over a stubbed [DiskLruCache] a real one cannot be pushed into. */
@RunWith(RobolectricTestRunner::class)
internal class ChapterCacheFailureTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val disk: DiskLruCache = mockk(relaxed = true)
    private val dir: File = File(context.cacheDir, "stub_cache").apply { mkdirs() }
    private lateinit var cache: ChapterCache

    @Before
    fun setUp() {
        mockkStatic(DiskLruCache::class)
        every { DiskLruCache.open(any(), any(), any(), any()) } returns disk
        every { disk.directory } returns dir
        val preferences = ReaderPreferences(MapPreferenceStore())
        cache = ChapterCache(context = context, json = Json, readerPreferences = preferences)
    }

    @After
    fun tearDown() {
        unmockkAll()
        dir.deleteRecursively()
    }

    @Test
    fun busyEntryIsSkipped() {
        every { disk.edit(any()) } returns null
        cache.putPageListToCache(Chapter.create(), emptyList())
        cache.putImageToCache("https://example.org/p.jpg", imageResponse("x"))
        verify(exactly = 0) { disk.flush() }
    }

    @Test
    fun failedPageListWriteIsIgnored() {
        val editor = mockk<DiskLruCache.Editor>(relaxed = true)
        every { disk.edit(any()) } returns editor
        every { editor.newOutputStream(0) } throws IOException("disk full")
        cache.putPageListToCache(Chapter.create(), emptyList())
        verify { editor.abortUnlessCommitted() }
    }

    @Test
    fun failedImageWriteIsRethrown() {
        val editor = mockk<DiskLruCache.Editor>(relaxed = true)
        every { disk.edit(any()) } returns editor
        every { editor.newOutputStream(0) } throws IOException("disk full")
        shouldThrow<IOException> { cache.putImageToCache("https://example.org/p.jpg", imageResponse("x")) }
        verify { editor.abortUnlessCommitted() }
    }

    @Test
    fun failedEditIsHandled() {
        every { disk.edit(any()) } throws IOException("journal")
        cache.putPageListToCache(Chapter.create(), emptyList())
        shouldThrow<IOException> { cache.putImageToCache("https://example.org/p.jpg", imageResponse("x")) }
    }

    @Test
    fun journalEntryWithoutFile() {
        every { disk.get(any()) } returns mockk(relaxed = true)
        cache.isImageInCache("https://example.org/p.jpg") shouldBe false
        cache.getImageFile("https://example.org/p.jpg").writeText("x")
        cache.isImageInCache("https://example.org/p.jpg") shouldBe true
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
    }

    @Test
    fun emptyDirectoryClearsNothing() {
        dir.deleteRecursively()
        cache.clear() shouldBe 0
    }
}
