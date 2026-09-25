package eu.kanade.tachiyomi.data.cache

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import eu.kanade.tachiyomi.data.track.MapPreferenceStore
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.setting.cacheSize
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import tachiyomi.domain.chapter.model.Chapter

internal fun imageResponse(body: String): Response = Response.Builder()
    .request(Request.Builder().url("https://example.org/p.jpg").build())
    .protocol(Protocol.HTTP_1_1)
    .code(200)
    .message("OK")
    .body(body.toResponseBody())
    .build()

@RunWith(RobolectricTestRunner::class)
internal class ChapterCacheTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val preferences = ReaderPreferences(MapPreferenceStore())
    private val chapter = Chapter.create().copy(mangaId = 1L, url = "/c/1")
    private lateinit var cache: ChapterCache

    @Before
    fun setUp() {
        context.cacheDir.resolve("chapter_disk_cache").deleteRecursively()
        cache = ChapterCache(context = context, json = Json, readerPreferences = preferences)
    }

    @Test
    fun pageListRoundTrips() {
        val pages = listOf(Page(index = 0, url = "/p0"), Page(index = 1, url = "/p1"))
        cache.putPageListToCache(chapter, pages)
        cache.getPageListFromCache(chapter).map { it.url } shouldBe listOf("/p0", "/p1")
    }

    @Test
    fun imageRoundTrips() {
        cache.isImageInCache("https://example.org/p.jpg") shouldBe false
        cache.putImageToCache("https://example.org/p.jpg", imageResponse("jpeg"))
        cache.isImageInCache("https://example.org/p.jpg") shouldBe true
        cache.getImageFile("https://example.org/p.jpg").readText() shouldBe "jpeg"
    }

    @Test
    fun journalWithoutFileIsNotCached() {
        cache.putImageToCache("https://example.org/p.jpg", imageResponse("jpeg"))
        cache.getImageFile("https://example.org/p.jpg").delete()
        cache.isImageInCache("https://example.org/p.jpg") shouldBe false
    }

    @Test
    fun clearKeepsTheJournal() {
        cache.putImageToCache("https://example.org/a.jpg", imageResponse("a"))
        cache.putImageToCache("https://example.org/b.jpg", imageResponse("b"))
        cache.readableSize.isNotEmpty() shouldBe true
        cache.clear() shouldBe 2
        cache.isImageInCache("https://example.org/a.jpg") shouldBe false
    }

    @Test
    fun resizingReopensTheCache() {
        cache.putImageToCache("https://example.org/a.jpg", imageResponse("a"))
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        preferences.cacheSize.set("150")
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        cache.isImageInCache("https://example.org/a.jpg") shouldBe true
    }
}
