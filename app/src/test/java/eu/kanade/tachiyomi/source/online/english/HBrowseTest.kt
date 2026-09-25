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
import exh.metadata.metadata.HBrowseSearchMetadata
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

private const val FULL = """
<html><body><div id="main">
<table class="listTable"><tr><td>Title</td><td>Comic Title</td></tr><tr><td>Length</td><td>24 pages</td></tr>
<tr><td>Artist</td><td><a>Foo</a><a>Bar</a></td></tr><tr><td>only one cell</td></tr></table>
<h2>Categories</h2>
<table class="listTable"><tr><td>Genre</td><td><a>Comedy</a></td></tr></table>
</div></body></html>
"""

@RunWith(RobolectricTestRunner::class)
internal class HBrowseTest {
    private val harness = SourceTestHarness()
    private lateinit var source: HBrowse

    @Before
    fun setUp() {
        harness.install()
        harness.serveMetadataSource()
        source = HBrowse(FakeDelegateSource(harness.baseUrl, name = "HBrowse"), harness.application)
    }

    @After
    fun tearDown() = harness.uninstall()

    @Test
    fun identity() {
        source.lang shouldBe "en"
        source.metaClass shouldBe HBrowseSearchMetadata::class
        source.newMetaInstance().javaClass shouldBe HBrowseSearchMetadata::class.java
        source.matchingHosts shouldContainExactly listOf("www.hbrowse.com", "hbrowse.com")
    }

    @Test
    fun fullPage() {
        val meta = HBrowseSearchMetadata()
        runBlocking { source.parseIntoMetadata(meta, jsoup(FULL, "${harness.baseUrl}/thumbnails/12345/c00001/")) }
        meta.hbUrl shouldBe "/12345/c00001/"
        meta.hbId shouldBe 12_345L
        meta.title shouldBe "Comic Title"
        meta.length shouldBe 24
        meta.tags shouldContainExactly listOf(
            rawTag("artist", "Foo", HBrowseSearchMetadata.TAG_TYPE_DEFAULT),
            rawTag("artist", "Bar", HBrowseSearchMetadata.TAG_TYPE_DEFAULT),
            rawTag("genre", "Comedy", HBrowseSearchMetadata.TAG_TYPE_DEFAULT),
        )
    }

    @Test
    fun missingTablesFail() {
        val meta = HBrowseSearchMetadata()
        val location = "${harness.baseUrl}/thumbnails/1/c00001/"
        val noCategories = FULL.substringBefore("<h2>") + "</div></body></html>"
        shouldThrow<IllegalStateException> {
            runBlocking { source.parseIntoMetadata(meta, jsoup(noCategories, location)) }
        }
        val onlyCategories = "<div id=\"main\"><h2>Categories</h2>" + FULL.substringAfter("<h2>Categories</h2>")
        shouldThrow<IllegalStateException> {
            runBlocking { source.parseIntoMetadata(meta, jsoup(onlyCategories, location)) }
        }
    }

    @Test
    fun mapUrlToMangaUrl() {
        runBlocking { source.mapUrlToMangaUrl(Uri.parse("https://www.hbrowse.com/12345/c00001")) } shouldBe
            "/12345/c00001/"
        runBlocking { source.mapUrlToMangaUrl(Uri.parse("https://www.hbrowse.com")) }.shouldBeNull()
    }

    @Test
    fun searchAndDetailsDelegate() {
        runBlocking { source.getSearchManga(1, "q", FilterList()) }.mangas.single().url shouldBe "/search/1/q"
        val rx = source.invokeDeclared(HBrowse::class, "fetchSearchManga", listOf(2, "r", FilterList()))
        ((rx as rx.Observable<*>).toBlocking().first() as MangasPage).mangas.single().url shouldBe "/search/2/r"
        harness.enqueue(FULL)
        val manga = sManga("/thumbnails/12345/c00001/")
        val details = source.invokeDeclared(HBrowse::class, "fetchMangaDetails", listOf(manga))
        ((details as rx.Observable<*>).toBlocking().first() as SManga).title shouldBe "Comic Title"
        harness.takeRequest().target shouldBe "/thumbnails/12345/c00001/"
    }
}
