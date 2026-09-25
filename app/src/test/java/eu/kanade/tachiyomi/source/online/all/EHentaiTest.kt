package eu.kanade.tachiyomi.source.online.all

import android.net.Uri
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.SourceTestHarness
import eu.kanade.tachiyomi.source.online.fixture
import eu.kanade.tachiyomi.source.online.invokeDeclared
import eu.kanade.tachiyomi.source.online.sManga
import exh.metadata.metadata.EHentaiSearchMetadata
import exh.source.EH_SOURCE_ID
import exh.source.EXH_SOURCE_ID
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.unmockkStatic
import kotlinx.coroutines.runBlocking
import okhttp3.CacheControl
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.core.common.util.lang.runAsObservable

private const val LIST = "eu/kanade/tachiyomi/source/online/all/eh_list_extended_layout.html"

@RunWith(RobolectricTestRunner::class)
internal class EHentaiTest {
    private val harness = SourceTestHarness()
    private lateinit var source: EHentai

    @Before
    fun setUp() {
        harness.install()
        source = harness.ehentai()
    }

    @After
    fun tearDown() {
        unmockkAll()
        harness.uninstall()
    }

    @Test
    fun identityPerSite() {
        source.id shouldBe EH_SOURCE_ID
        source.name shouldBe "E-Hentai"
        source.baseUrl shouldBe "https://e-hentai.org"
        source.lang shouldBe "all"
        source.supportsLatest shouldBe true
        source.matchingHosts shouldContainExactly listOf("g.e-hentai.org", "e-hentai.org")
        source.metaClass shouldBe EHentaiSearchMetadata::class
        source.newMetaInstance().javaClass shouldBe EHentaiSearchMetadata::class.java
        val exh = harness.ehentai(exh = true)
        exh.id shouldBe EXH_SOURCE_ID
        exh.name shouldBe "ExHentai"
        exh.baseUrl shouldBe "https://exhentai.org"
        exh.matchingHosts shouldContainExactly listOf("exhentai.org")
        EHentai.buildCookies(mapOf("a b" to "c&d")) shouldBe "a+b=c%26d"
        EHentai.JSON.toString() shouldBe "application/json; charset=utf-8"
    }

    // `getPopularManga` and friends defer to `CatalogueSource`, which calls back into this class's
    // `fetch*`, which wrap `get*` again: a real recursion in main code. The bridge is stubbed so
    // each side runs exactly once.
    private fun stubBridgeAgainstRecursion(page: MangasPage): CapturingSlot<suspend () -> Any> {
        val block = slot<suspend () -> Any>()
        mockkStatic("tachiyomi.core.common.util.lang.RxCoroutineBridgeKt")
        every { runAsObservable(any(), capture(block)) } returns rx.Observable.just(page)
        return block
    }

    @Test
    fun listingsGoThroughTheBridge() {
        val page = MangasPage(listOf(sManga("/g/1/a/")), false)
        val block = stubBridgeAgainstRecursion(page)
        runBlocking { source.getPopularManga(1) } shouldBe page
        runBlocking { block.captured() } shouldBe page
        runBlocking { source.getLatestUpdates(2) } shouldBe page
        runBlocking { block.captured() } shouldBe page
        runBlocking { source.getSearchManga(1, "q", FilterList()) } shouldBe page
        runBlocking { block.captured() } shouldBe page
        unmockkStatic("tachiyomi.core.common.util.lang.RxCoroutineBridgeKt")
    }

    @Test
    fun rxListingsDelegate() {
        val page = MangasPage(emptyList(), false)
        val block = stubBridgeAgainstRecursion(page)
        (source.invokeDeclared(EHentai::class, "fetchPopularManga", listOf(1)) is rx.Observable<*>) shouldBe true
        (source.invokeDeclared(EHentai::class, "fetchLatestUpdates", listOf(1)) is rx.Observable<*>) shouldBe true
        val search = source.invokeDeclared(EHentai::class, "fetchSearchManga", listOf(1, "q", FilterList()))
        (search is rx.Observable<*>) shouldBe true
        block.isCaptured shouldBe true
        unmockkStatic("tachiyomi.core.common.util.lang.RxCoroutineBridgeKt")
    }

    @Test
    fun rxDetailsChaptersPages() {
        harness.enqueue(fixture("eu/kanade/tachiyomi/source/online/all/eh_gallery_full.html"))
        val details = source.invokeDeclared(EHentai::class, "fetchMangaDetails", listOf(sManga("/g/123/abc/")))
        (details as rx.Observable<*>).toBlocking().first().let { (it as SManga).title shouldBe "Main Title" }
        harness.enqueue(galleryHtml("Root", null))
        val chapters = source.invokeDeclared(EHentai::class, "fetchChapterList", listOf(sManga("/g/100/parent/")))
        ((chapters as rx.Observable<*>).toBlocking().first() as List<*>).size shouldBe 1
        harness.enqueue("""<div id="gdt"><a href="/s/a/1"><img title="Page 1: a"></a></div>""")
        val chapter = SChapter(url = "/g/1/a/", name = "c")
        val pages = source.invokeDeclared(EHentai::class, "fetchPageList", listOf(chapter))
        ((pages as rx.Observable<*>).toBlocking().first() as List<*>).size shouldBe 1
        harness.enqueue("""<img id="img" src="https://host/1.jpg">""")
        val page = Page(0, "https://e-hentai.org/s/a/1")
        val image = source.invokeDeclared(EHentai::class, "fetchImageUrl", listOf(page))
        (image as rx.Observable<*>).toBlocking().first() shouldBe "https://host/1.jpg"
    }

    @Test
    fun pageListFollowsNextLinks() {
        harness.enqueue(
            """<div id="gdt"><a href="/s/a/1"><img title="Page 1: a"></a></div>""" +
                """<a onclick="return false" href="https://e-hentai.org/g/1/a/?p=1">&gt;</a>""",
        )
        harness.enqueue("""<div id="gdt"><a href="/s/b/2"><img title="Page 2: b"></a></div>""")
        val pages = runBlocking { source.getPageList(SChapter(url = "/g/1/a/", name = "c")) }
        pages.map { it.url } shouldContainExactly listOf("/s/a/1", "/s/b/2")
        pages.map { it.index } shouldContainExactly listOf(0, 1)
        harness.takeRequest().target shouldBe "/g/1/a/"
        harness.takeRequest().target shouldBe "/g/1/a/?p=1"
    }

    @Test
    fun imageUrlAndHelpers() {
        harness.enqueue("""<img id="img" src="https://host/1.jpg">""")
        runBlocking { source.getImageUrl(Page(0, "https://e-hentai.org/s/a/1")) } shouldBe "https://host/1.jpg"
        source.headers["Cookie"] shouldBe "sl=dm_2; nw=1"
        source.getFilterList().size shouldBe 9
        source.cleanMangaUrl("https://e-hentai.org/g/1/a/?x=1") shouldBe "/g/1/a/?nw=always"
        runBlocking { source.mapUrlToMangaUrl(Uri.parse("https://e-hentai.org/g/1/a/")) } shouldBe
            "https://e-hentai.org/g/1/a/"
        EHentai.GalleryNotFoundException(IllegalStateException("c")).cause?.message shouldBe "c"
    }

    @Test
    fun unusedHelpersThrow() {
        val response = eu.kanade.tachiyomi.source.online.cannedResponse("")
        for (name in listOf("mangaDetailsParse", "chapterListParse", "pageListParse", "imageUrlParse")) {
            shouldThrow<UnsupportedOperationException> { source.invokeDeclared(EHentai::class, name, listOf(response)) }
        }
        val request = source.invokeDeclared(EHentai::class, "popularMangaRequest", listOf(1)) as okhttp3.Request
        request.url.toString() shouldBe "https://e-hentai.org/popular"
        val latest = source.invokeDeclared(EHentai::class, "latestUpdatesRequest", listOf(3)) as okhttp3.Request
        latest.url.toString() shouldBe "https://e-hentai.org/?next=3"
        val search = source.invokeDeclared(EHentai::class, "searchMangaRequest", listOf(1, "q", FilterList()))
        (search as okhttp3.Request).url.toString() shouldBe "https://e-hentai.org/?f_apply=Apply%2BFilter&f_search=q"
        for (name in listOf("popularMangaParse", "searchMangaParse", "latestUpdatesParse")) {
            val list = eu.kanade.tachiyomi.source.online.cannedResponse(fixture(LIST), "https://e-hentai.org/")
            (source.invokeDeclared(EHentai::class, name, listOf(list)) as MangasPage).mangas.size shouldBe 3
        }
    }

    @Test
    fun previewsAndUpdate() {
        harness.enqueue(
            """<div id="gdt"><div><a><img alt="1" src="https://ehgt.org/t/1.jpg"></a></div></div>""" +
                """<table class="ptt"><tbody><tr><td class="ptdd">1</td></tr></tbody></table>""",
        )
        runBlocking { source.getPagePreviewList(sManga("/g/1/a/"), emptyList(), 1) }.pagePreviews.size shouldBe 1
        harness.enqueueBytes(byteArrayOf(1, 2, 3), "image/jpeg")
        val preview = eu.kanade.tachiyomi.source.PagePreviewInfo(1, "https://ehgt.org/t/1.jpg")
        runBlocking { source.fetchPreviewImage(preview, CacheControl.FORCE_NETWORK) }.body.bytes().size shouldBe 3
        harness.enqueueBytes(byteArrayOf(1), "image/jpeg")
        runBlocking { source.fetchPreviewImage(preview) }.body.bytes().size shouldBe 1
        val manga = sManga("/g/1/a/")
        val update = runBlocking {
            source.getMangaUpdate(manga, emptyList(), fetchDetails = false, fetchChapters = false)
        }
        update.manga shouldBe manga
    }
}
