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
import exh.metadata.metadata.RaisedSearchMetadata
import exh.metadata.metadata.TsuminoSearchMetadata
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
<html><head><meta property="og:title" content=" Book Title "></head><body>
<div id="Artist"><a data-define=" A | B ">x</a></div>
<div id="Uploader"><a> uploader </a></div>
<div id="Uploaded">2020 Jan 05</div>
<div id="Pages">24</div>
<div id="Rating">4.5 (10 users / 3 favs)</div>
<div id="Category"><a data-define="Doujinshi"></a></div>
<div id="Collection"><a data-define="Coll"></a></div>
<div id="Group"><a data-define="Grp"></a></div>
<div id="Parody"><a data-define="P1"></a><a data-define="P2"></a></div>
<div id="Character"><a data-define="C1"></a></div>
<div id="Tag"><a data-define="T1"></a><a data-define=" T2 "></a></div>
</body></html>
"""

@RunWith(RobolectricTestRunner::class)
internal class TsuminoTest {
    private val harness = SourceTestHarness()
    private lateinit var source: Tsumino

    @Before
    fun setUp() {
        harness.install()
        harness.serveMetadataSource()
        source = Tsumino(FakeDelegateSource(harness.baseUrl, name = "Tsumino"), harness.application)
    }

    @After
    fun tearDown() = harness.uninstall()

    private fun parse(html: String, url: String = "https://www.tsumino.com/entry/12345") =
        TsuminoSearchMetadata().also { runBlocking { source.parseIntoMetadata(it, jsoup(html, url)) } }

    @Test
    fun identity() {
        source.lang shouldBe "en"
        source.metaClass shouldBe TsuminoSearchMetadata::class
        source.newMetaInstance().javaClass shouldBe TsuminoSearchMetadata::class.java
        source.matchingHosts shouldContainExactly listOf("www.tsumino.com", "tsumino.com")
    }

    @Test
    fun fullPage() {
        val meta = parse(FULL)
        meta.tmId shouldBe 12_345
        meta.title shouldBe "Book Title"
        meta.artist shouldBe "A | B"
        meta.uploader shouldBe "uploader"
        meta.uploadDate shouldBe Tsumino.TM_DATE_FORMAT.parse("2020 Jan 05")!!.time
        meta.length shouldBe 24
        meta.ratingString shouldBe "4.5 (10 users / 3 favs)"
        meta.averageRating shouldBe 4.5f
        meta.userRatings shouldBe 10L
        meta.favorites shouldBe 3L
        meta.category shouldBe "Doujinshi"
        meta.collection shouldBe "Coll"
        meta.group shouldBe "Grp"
        meta.parody shouldContainExactly listOf("P1", "P2")
        meta.character shouldContainExactly listOf("C1")
        meta.tags shouldContainExactly listOf(
            rawTag("artist", "A", TsuminoSearchMetadata.TAG_TYPE_DEFAULT),
            rawTag("artist", "B", TsuminoSearchMetadata.TAG_TYPE_DEFAULT),
            rawTag("artist", "A | B", RaisedSearchMetadata.TAG_TYPE_VIRTUAL),
            rawTag("genre", "Doujinshi", RaisedSearchMetadata.TAG_TYPE_VIRTUAL),
            rawTag("collection", "Coll", TsuminoSearchMetadata.TAG_TYPE_DEFAULT),
            rawTag("group", "Grp", TsuminoSearchMetadata.TAG_TYPE_DEFAULT),
            rawTag("parody", "P1", TsuminoSearchMetadata.TAG_TYPE_DEFAULT),
            rawTag("parody", "P2", TsuminoSearchMetadata.TAG_TYPE_DEFAULT),
            rawTag("character", "C1", TsuminoSearchMetadata.TAG_TYPE_DEFAULT),
            rawTag("tags", "T1", TsuminoSearchMetadata.TAG_TYPE_DEFAULT),
            rawTag("tags", "T2", TsuminoSearchMetadata.TAG_TYPE_DEFAULT),
        )
    }

    @Test
    fun sparsePage() {
        val meta = parse("""<html><body><div id="Rating"> </div><div id="Pages">x</div></body></html>""")
        meta.tmId shouldBe 12_345
        meta.title.shouldBeNull()
        meta.artist.shouldBeNull()
        meta.uploader.shouldBeNull()
        meta.uploadDate.shouldBeNull()
        meta.length.shouldBeNull()
        meta.ratingString shouldBe ""
        meta.averageRating.shouldBeNull()
        meta.category.shouldBeNull()
        meta.parody.isEmpty() shouldBe true
        meta.tags.isEmpty() shouldBe true
        parse("""<html><body><div id="Rating">bad</div></body></html>""").averageRating.shouldBeNull()
        val empty = parse("""<div id="Artist"></div><div id="Uploader"></div><div id="Parody"></div>""")
        empty.artist.shouldBeNull()
        empty.uploader.shouldBeNull()
    }

    @Test
    fun mapUrlToMangaUrl() {
        val mapped = runBlocking { source.mapUrlToMangaUrl(Uri.parse("https://www.tsumino.com/Read/Index/123/1")) }
        mapped shouldBe "https://tsumino.com/Book/Info/1"
        runBlocking { source.mapUrlToMangaUrl(Uri.parse("https://tsumino.com/Book/Info/123")) } shouldBe
            "https://tsumino.com/Book/Info/123"
        runBlocking { source.mapUrlToMangaUrl(Uri.parse("https://tsumino.com/entry/9")) } shouldBe
            "https://tsumino.com/Book/Info/9"
        runBlocking { source.mapUrlToMangaUrl(Uri.parse("https://tsumino.com/Search/x")) }.shouldBeNull()
        runBlocking { source.mapUrlToMangaUrl(Uri.parse("https://tsumino.com")) }.shouldBeNull()
    }

    @Test
    fun searchAndDetailsDelegate() {
        runBlocking { source.getSearchManga(1, "q", FilterList()) }.mangas.single().url shouldBe "/search/1/q"
        val rx = source.invokeDeclared(Tsumino::class, "fetchSearchManga", listOf(2, "r", FilterList()))
        ((rx as rx.Observable<*>).toBlocking().first() as MangasPage).mangas.single().url shouldBe "/search/2/r"
        harness.enqueue(FULL)
        val details = source.invokeDeclared(Tsumino::class, "fetchMangaDetails", listOf(sManga("/entry/12345")))
        ((details as rx.Observable<*>).toBlocking().first() as SManga).title shouldBe "Book Title"
        harness.takeRequest().target shouldBe "/entry/12345"
    }
}
