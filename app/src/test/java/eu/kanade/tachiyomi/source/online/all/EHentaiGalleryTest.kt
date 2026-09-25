package eu.kanade.tachiyomi.source.online.all

import android.net.Uri
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.online.SourceTestHarness
import eu.kanade.tachiyomi.source.online.cannedResponse
import eu.kanade.tachiyomi.source.online.fixture
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
internal class EHentaiGalleryTest {
    private val harness = SourceTestHarness()
    private lateinit var source: EHentai

    @Before
    fun setUp() {
        harness.install()
        source = harness.ehentai()
    }

    @After
    fun tearDown() = harness.uninstall()

    @Test
    fun imageUrlWithRetryToken() {
        val page = Page(0, "https://e-hentai.org/s/tok/1-1")
        val html = """<img id="img" src="https://host/1.jpg"><a id="loadfail" onclick="return nl('123-456')">x</a>"""
        source.realImageUrlParse(cannedResponse(html), page) shouldBe "https://host/1.jpg"
        page.url shouldBe "https://e-hentai.org/s/tok/1-1?nl=123-456"
    }

    @Test
    fun imageUrlWithoutRetryToken() {
        val page = Page(0, "https://e-hentai.org/s/tok/1-1")
        source.realImageUrlParse(cannedResponse("""<img id="img" src="https://host/1.jpg">"""), page) shouldBe
            "https://host/1.jpg"
        page.url shouldBe "https://e-hentai.org/s/tok/1-1"
    }

    @Test
    fun imageUrlQuotaExceeded() {
        val html = """<img id="img" src="https://ehgt.org/g/509.gif">"""
        shouldThrow<IOException> { source.realImageUrlParse(cannedResponse(html), Page(0, "u")) }.message shouldBe
            "Exceeded page quota"
    }

    @Test
    fun favoritesWalkPages() {
        val page1 = fixture("eu/kanade/tachiyomi/source/online/all/eh_list_thumb_layout.html")
            .replace(
                "</body>",
                """<div class="fp"><div></div><div></div><div>Fav A</div></div>""" +
                    """<div class="fp fps"><div></div><div></div><div>skipped</div></div></body>""",
            )
        val page2 = fixture("eu/kanade/tachiyomi/source/online/all/eh_list_extended_layout.html")
        harness.enqueue(page1)
        harness.enqueue(page2)
        val (mangas, names) = runBlocking { source.fetchFavorites() }
        harness.takeRequest().target shouldBe "/favorites.php"
        harness.takeRequest().target shouldBe "/favorites.php?next=7"
        mangas.size shouldBe 6
        names shouldContainExactly listOf("Fav A")
    }

    @Test
    fun favoritesEmpty() {
        harness.enqueue("""<table class="itg"><tbody></tbody></table>""")
        val (mangas, names) = runBlocking { source.fetchFavorites() }
        mangas.isEmpty() shouldBe true
        names.isEmpty() shouldBe true
    }

    @Test
    fun galleryUrlFromPage() {
        harness.enqueue("""{"tokenlist":[{"gid":123,"token":"abc"}]}""")
        val uri = Uri.parse("https://e-hentai.org/s/pagetoken/123-4")
        source.getGalleryUrlFromPage(uri) shouldBe "https://e-hentai.org/g/123/abc/"
        val request = harness.takeRequest()
        request.method shouldBe "POST"
        request.target shouldBe "/api.php"
        request.body?.utf8() shouldBe """{"method":"gtoken","pagelist":[[123,"pagetoken",4]]}"""
    }

    @Test
    fun mangaUrlFromUri() {
        runBlocking { source.mangaUrlFromUri(Uri.parse("https://e-hentai.org/g/1/a/")) } shouldBe
            "https://e-hentai.org/g/1/a/"
        harness.enqueue("""{"tokenlist":[{"gid":9,"token":"t"}]}""")
        runBlocking { source.mangaUrlFromUri(Uri.parse("https://exhentai.org/s/p/9-2")) } shouldBe
            "https://exhentai.org/g/9/t/"
        runBlocking { source.mangaUrlFromUri(Uri.parse("https://e-hentai.org/tag/x")) }.shouldBeNull()
        runBlocking { source.mangaUrlFromUri(Uri.parse("https://e-hentai.org")) }.shouldBeNull()
    }
}
