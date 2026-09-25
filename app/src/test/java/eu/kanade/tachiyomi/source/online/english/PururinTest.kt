package eu.kanade.tachiyomi.source.online.english

import android.net.Uri
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.FakeDelegateSource
import eu.kanade.tachiyomi.source.online.SourceTestHarness
import eu.kanade.tachiyomi.source.online.invokeDeclared
import eu.kanade.tachiyomi.source.online.jsoup
import eu.kanade.tachiyomi.source.online.rawTag
import eu.kanade.tachiyomi.source.online.sManga
import eu.kanade.tachiyomi.source.online.serveMetadataSource
import exh.metadata.metadata.PururinSearchMetadata
import exh.metadata.metadata.RaisedSearchMetadata
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private const val FULL = """
<html><body>
<a href="https://pururin.me/gallery/12345/short-link"><span itemprop="name">Title</span></a>
<div class="content-wrapper">
<div class="title"><h1>Gallery Title</h1></div><div class="alt-title">Alt</div>
<table class="table-gallery-info"><tbody>
<tr><td>Pages</td><td>24 (12 MB)</td></tr>
<tr><td>Ratings</td><td><span itemprop="ratingCount" content="10"></span><span itemprop="ratingValue" content="4.5"></span></td></tr>
<tr><td>Uploader</td><td><a href="https://pururin.me/uploader/someone">Some One</a></td></tr>
<tr><td>Artist</td><td><a href="https://pururin.me/tags/artist/1/foo.html">Foo</a></td></tr>
<tr><td>Category</td><td><a href="https://pururin.me/tags/category/2/doujinshi.html">Doujinshi</a></td></tr>
</tbody></table>
</div>
<div class="cover-wrapper"><v-lazy-image src="//cdn.pururin.me/cover.jpg"></v-lazy-image></div>
</body></html>
"""

@RunWith(RobolectricTestRunner::class)
internal class PururinTest {
    private val harness = SourceTestHarness()
    private lateinit var source: Pururin

    @Before
    fun setUp() {
        harness.install()
        harness.serveMetadataSource()
        source = Pururin(FakeDelegateSource(harness.baseUrl, name = "Pururin"), harness.application)
    }

    @After
    fun tearDown() = harness.uninstall()

    @Test
    fun identity() {
        source.lang shouldBe "en"
        source.metaClass shouldBe PururinSearchMetadata::class
        source.newMetaInstance().javaClass shouldBe PururinSearchMetadata::class.java
        source.matchingHosts shouldContainExactly listOf("pururin.me")
    }

    @Test
    fun fullPage() {
        val meta = PururinSearchMetadata()
        runBlocking { source.parseIntoMetadata(meta, jsoup(FULL, "https://pururin.me/gallery/12345/short-link")) }
        meta.prId shouldBe 12_345
        meta.prShortLink shouldBe "short-link"
        meta.title shouldBe "Gallery Title"
        meta.altTitle shouldBe "Alt"
        meta.thumbnailUrl shouldBe "https://cdn.pururin.me/cover.jpg"
        meta.pages shouldBe 24
        meta.fileSize shouldBe "12 MB"
        meta.ratingCount shouldBe 10
        meta.averageRating shouldBe 4.5
        meta.uploaderDisp shouldBe "Some One"
        meta.uploader shouldBe "someone"
        meta.tags shouldContainExactly listOf(
            rawTag("artist", "foo", PururinSearchMetadata.TAG_TYPE_DEFAULT),
            rawTag("category", "doujinshi", RaisedSearchMetadata.TAG_TYPE_VIRTUAL),
        )
    }

    @Test
    fun pageWithoutAltTitle() {
        val meta = PururinSearchMetadata()
        val html = FULL.replace("""<div class="alt-title">Alt</div>""", "")
        runBlocking { source.parseIntoMetadata(meta, jsoup(html, "")) }
        meta.altTitle.shouldBeNull()
    }

    @Test
    fun mapUrlToMangaUrl() {
        runBlocking { source.mapUrlToMangaUrl(Uri.parse("https://pururin.me/gallery/12345/short-link")) } shouldBe
            "https://pururin.me/gallery/12345/short-link"
        runBlocking { source.mapUrlToMangaUrl(Uri.parse("https://pururin.me/x")) } shouldBe
            "https://pururin.me/gallery/null/x"
    }

    @Test
    fun searchByIdBecomesUrlImport() {
        val plain = Pururin(FakeDelegateSource("nohttp.example", name = "Pururin"), harness.application)
        runBlocking { plain.getSearchManga(1, "id:123", FilterList()) }.mangas.single().url shouldBe "/search/1/id:123"
        runBlocking { plain.getSearchManga(1, " 77 ", FilterList()) }.mangas.single().url shouldBe "/search/1/ 77 "
        runBlocking { source.getSearchManga(1, "words", FilterList()) }.mangas.single().url shouldBe "/search/1/words"
        val rx = source.invokeDeclared(Pururin::class, "fetchSearchManga", listOf(2, "r", FilterList()))
        ((rx as rx.Observable<*>).toBlocking().first() as MangasPage).mangas.single().url shouldBe "/search/2/r"
    }

    @Test
    fun rxDetailsGoThroughDelegate() {
        harness.enqueue(FULL)
        val manga = sManga("/gallery/12345/short-link")
        val details = source.invokeDeclared(Pururin::class, "fetchMangaDetails", listOf(manga))
        ((details as rx.Observable<*>).toBlocking().first() as SManga).title shouldBe "Gallery Title"
        harness.takeRequest().target shouldBe "/gallery/12345/short-link"
    }
}
