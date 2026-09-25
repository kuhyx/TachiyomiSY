package eu.kanade.tachiyomi.source.online.all

import eu.kanade.tachiyomi.source.online.SourceTestHarness
import eu.kanade.tachiyomi.source.online.fixture
import eu.kanade.tachiyomi.source.online.sChapter
import eu.kanade.tachiyomi.source.online.sManga
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
internal class EHentaiDetailsTest {
    private val harness = SourceTestHarness()
    private lateinit var source: EHentai
    private val gallery = fixture("eu/kanade/tachiyomi/source/online/all/eh_gallery_full.html")

    @Before
    fun setUp() {
        harness.install()
        source = harness.ehentai()
    }

    @After
    fun tearDown() = harness.uninstall()

    @Test
    fun detailsParseGalleryPage() {
        harness.enqueue(gallery)
        val manga = runBlocking { source.getMangaDetails(sManga("/g/123/abc/")) }
        harness.takeRequest().target shouldBe "/g/123/abc/"
        manga.title shouldBe "Main Title"
        manga.initialized shouldBe true
        manga.thumbnail_url shouldBe "https://ehgt.org/cover.jpg"
    }

    @Test
    fun detailsPullToNewestVersion() {
        val newer = listOf("https://e-hentai.org/g/300/newer/" to "2023-05-06 07:08")
        harness.enqueue(galleryHtml("Old", null, newer))
        harness.enqueue(gallery)
        val manga = runBlocking { source.getMangaDetails(sManga("/g/100/parent/")) }
        harness.takeRequest().target shouldBe "/g/100/parent/"
        harness.takeRequest().target shouldBe "/g/300/newer/?nw=always"
        manga.title shouldBe "Main Title"
    }

    @Test
    fun detailsPullToRootDisabled() {
        harness.store.getBoolean("eh_debug_toggle_pull_to_root_when_loading_exh_manga_details", true).set(false)
        val newer = listOf("https://e-hentai.org/g/300/newer/" to "2023-05-06 07:08")
        harness.enqueue(galleryHtml("Old", null, newer))
        runBlocking { source.getMangaDetails(sManga("/g/100/parent/")) }.title shouldBe "Old"
        harness.server.requestCount shouldBe 1
    }

    @Test
    fun detailsNotFound() {
        harness.enqueue("", code = 404)
        val error = shouldThrow<EHentai.GalleryNotFoundException> {
            runBlocking { source.getMangaDetails(sManga("/g/1/a/")) }
        }
        error.message shouldBe "Gallery not found!"
        error.cause?.message shouldBe "Async stacktrace"
    }

    @Test
    fun detailsOtherError() {
        harness.enqueue("", code = 500)
        val error = shouldThrow<IOException> { runBlocking { source.getMangaDetails(sManga("/g/1/a/")) } }
        error.message shouldBe "HTTP error 500"
    }

    @Test
    fun updateFetchesBoth() {
        // Details and chapters run concurrently and ask for the same page, so the body is served by path.
        harness.answer { galleryHtml("Root", null) }
        val chapters = listOf(sChapter("/old/"))
        val update = runBlocking {
            source.getMangaUpdate(sManga("/g/123/abc/"), chapters, fetchDetails = true, fetchChapters = true) {}
        }
        update.manga.title shouldBe "Root"
        update.chapters.map { it.name } shouldBe listOf("v1: Root")
    }

    @Test
    fun updateFetchesNothing() {
        val manga = sManga("/g/123/abc/")
        val chapters = listOf(sChapter("/old/"))
        val update = runBlocking {
            source.getMangaUpdate(manga, chapters, fetchDetails = false, fetchChapters = false) {}
        }
        update.manga shouldBe manga
        update.chapters shouldBe chapters
        harness.server.requestCount shouldBe 0
    }

    @Test
    fun updateFetchesOnlyDetails() {
        harness.enqueue(gallery)
        val chapters = listOf(sChapter("/old/"))
        val update = runBlocking {
            source.getMangaUpdate(sManga("/g/123/abc/"), chapters, fetchDetails = true, fetchChapters = false)
        }
        update.manga.title shouldBe "Main Title"
        update.chapters shouldBe chapters
    }

    @Test
    fun updateFetchesOnlyChapters() {
        harness.enqueue(galleryHtml("Root", null))
        val manga = sManga("/g/100/parent/")
        val update = runBlocking {
            source.getMangaUpdate(manga, emptyList(), fetchDetails = false, fetchChapters = true)
        }
        update.manga shouldBe manga
        update.chapters.map { it.name } shouldBe listOf("v1: Root")
    }
}
