package eu.kanade.tachiyomi.source.online.all

import eu.kanade.tachiyomi.source.online.SourceTestHarness
import eu.kanade.tachiyomi.source.online.jsoup
import eu.kanade.tachiyomi.source.online.sManga
import exh.eh.GalleryEntry
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

/** A gallery page with a "Parent:" row, [newer] versions and the given posted date. */
internal fun galleryHtml(title: String, parentHref: String?, newer: List<Pair<String, String>> = emptyList()): String {
    val parent = if (parentHref == null) "None" else """<a href="$parentHref">${parentHref.substringAfter("/g/")}</a>"""
    val versions = newer.joinToString("<br>") { (href, date) -> """<a href="$href">$title v</a>, added $date""" }
    return """
        <html><body><h1 id="gn">$title</h1>
        <div id="gdd"><table>
        <tr><td class="gdt1">Posted:</td><td class="gdt2">2023-01-02 03:04</td></tr>
        <tr><td class="gdt1">Parent:</td><td class="gdt2">$parent</td></tr>
        </table></div>
        <div id="gnd">$versions</div>
        </body></html>
    """.trimIndent()
}

@RunWith(RobolectricTestRunner::class)
internal class EHentaiChaptersTest {
    private val harness = SourceTestHarness()
    private val parents = FakeParentTable()
    private lateinit var source: EHentai

    @Before
    fun setUp() {
        harness.install()
        source = harness.ehentai(parents = parents)
    }

    @After
    fun tearDown() = harness.uninstall()

    @Test
    fun followsParentChainAndVersions() {
        harness.enqueue(galleryHtml("Child", "https://e-hentai.org/g/100/parent/"))
        val newer = listOf("https://e-hentai.org/g/300/newer/" to "2023-05-06 07:08")
        harness.enqueue(galleryHtml("Root", null, newer))
        var throttled = 0
        val chapters = runBlocking { source.getChapterList(sManga("/g/200/child/")) { throttled++ } }
        throttled shouldBe 2
        harness.takeRequest().target shouldBe "/g/200/child/"
        harness.takeRequest().target shouldBe "/g/100/parent/?nw=always"
        parents.entries[200] shouldBe GalleryEntry("100", "parent")
        chapters.map { it.name } shouldContainExactly listOf("v2: Root v", "v1: Root")
        chapters[0].url shouldBe "/g/300/newer/?nw=always"
        chapters[0].chapter_number shouldBe 2f
        chapters[0].date_upload shouldBe 1_683_356_880_000L
        chapters[0].scanlator shouldBe "300"
        chapters[1].url shouldBe "/g/100/parent/?nw=always"
        chapters[1].chapter_number shouldBe 1f
        chapters[1].date_upload shouldBe 1_672_628_640_000L
        chapters[1].scanlator shouldBe "100"
    }

    @Test
    fun cachedParentSkipsFetch() {
        parents.entries[200] = GalleryEntry("100", "parent")
        harness.enqueue(galleryHtml("Root", null))
        val chapters = runBlocking { source.getChapterList(sManga("/g/200/child/")) }
        harness.takeRequest().target shouldBe "/g/100/parent/?nw=always"
        chapters.map { it.name } shouldContainExactly listOf("v1: Root")
    }

    @Test
    fun onlyRootToggleStopsAtSelf() {
        harness.store.getBoolean("eh_debug_toggle_include_only_root_when_loading_exh_versions").set(true)
        val newer = listOf("https://e-hentai.org/g/300/newer/" to "2023-05-06 07:08")
        harness.enqueue(galleryHtml("Root", null, newer))
        val chapters = runBlocking { source.getChapterList(sManga("/g/100/parent/")) }
        chapters.map { it.name } shouldContainExactly listOf("v1: Root")
    }

    @Test
    fun missingDetailRowFails() {
        harness.enqueue("""<h1 id="gn">No table</h1><div id="gdd"></div>""")
        shouldThrow<NullPointerException> { runBlocking { source.getChapterList(sManga("/g/1/a/")) } }
    }

    @Test
    fun parseChapterPageSortsLayouts() {
        val html = """
            <div class="gdtm"><a href="/s/b/2"><img alt="2"></a></div>
            <div id="gdt"><a href="/s/a/1"><img title="Page 1: a.jpg"></a><a href="/s/c/3"><img title="Page 3: c"></a></div>
        """.trimIndent()
        source.parseChapterPage(jsoup(html, "")) shouldContainExactly listOf("/s/a/1", "/s/b/2", "/s/c/3")
        source.parseChapterPage(jsoup("<div></div>", "")).isEmpty() shouldBe true
    }

    @Test
    fun nextPageUrl() {
        val first = """<a onclick="return false" href="/g/1/a/?p=0">&lt;</a>"""
        val next = first + """<a onclick="return false" href="/g/1/a/?p=1">&gt;</a>"""
        source.nextPageUrl(jsoup(next, "")) shouldBe "/g/1/a/?p=1"
        val last = first + """<a onclick="return false" href="#">3</a>"""
        source.nextPageUrl(jsoup(last, "")).shouldBeNull()
        source.nextPageUrl(jsoup("<div></div>", "")).shouldBeNull()
    }

    @Test
    fun chapterPageRequestHeaders() {
        val request = source.chapterPageRequest("https://e-hentai.org/g/1/a/?p=1")
        request.url.toString() shouldBe "https://e-hentai.org/g/1/a/?p=1"
        request.headers.values("User-Agent") shouldBe listOf(SourceTestHarness.USER_AGENT, SourceTestHarness.USER_AGENT)
    }
}
