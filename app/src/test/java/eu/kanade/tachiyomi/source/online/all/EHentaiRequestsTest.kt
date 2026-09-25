package eu.kanade.tachiyomi.source.online.all

import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.MetadataMangasPage
import eu.kanade.tachiyomi.source.online.SourceTestHarness
import eu.kanade.tachiyomi.source.online.cannedResponse
import eu.kanade.tachiyomi.source.online.fixture
import eu.kanade.tachiyomi.source.online.sManga
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import okhttp3.CacheControl
import okhttp3.Headers.Companion.headersOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
internal class EHentaiRequestsTest {
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
    fun exGetPlain() {
        val request = source.exGet("https://e-hentai.org/x")
        request.url.toString() shouldBe "https://e-hentai.org/x"
        request.header("User-Agent") shouldBe SourceTestHarness.USER_AGENT
        request.header("Cookie") shouldBe "sl=dm_2; nw=1"
        request.cacheControl.toString() shouldBe "max-age=600"
    }

    @Test
    fun exGetNextAndPrev() {
        source.exGet("https://e-hentai.org/x", next = 5).url.toString() shouldBe "https://e-hentai.org/x?next=5"
        source.exGet("https://e-hentai.org/x", next = 1).url.toString() shouldBe "https://e-hentai.org/x"
        source.exGet("https://e-hentai.org/x", prev = 3).url.toString() shouldBe "https://e-hentai.org/x?prev=3"
        source.exGet("https://e-hentai.org/x", prev = 0).url.toString() shouldBe "https://e-hentai.org/x"
        source.exGet("https://e-hentai.org/x", next = 1, prev = 2).url.toString() shouldBe
            "https://e-hentai.org/x?prev=2"
        source.exGet("https://e-hentai.org/x", next = 4, prev = 2).url.toString() shouldBe
            "https://e-hentai.org/x?next=4"
    }

    @Test
    fun exGetExtraHeadersAndCache() {
        val request = source.exGet(
            "https://e-hentai.org/x",
            additionalHeaders = headersOf("X-A", "1", "X-A", "2"),
            cacheControl = CacheControl.FORCE_NETWORK,
        )
        request.headers.values("X-A") shouldBe listOf("1", "2")
        request.header("User-Agent") shouldBe SourceTestHarness.USER_AGENT
        request.cacheControl.noCache shouldBe true
    }

    @Test
    fun galleryRequestUsesBaseUrl() {
        source.galleryRequest(sManga("/g/1/a/")).url.toString() shouldBe "https://e-hentai.org/g/1/a/"
        harness.ehentai(exh = true).galleryRequest(sManga("/g/1/a/")).url.toString() shouldBe
            "https://exhentai.org/g/1/a/"
    }

    @Test
    fun genericMangaParse() {
        val html = fixture("eu/kanade/tachiyomi/source/online/all/eh_list_extended_layout.html")
        val page = source.genericMangaParse(cannedResponse(html, "https://e-hentai.org/?f_search=x"))
        page.mangas.size shouldBe 3
        page.hasNextPage shouldBe false
        page.mangasMetadata.size shouldBe 3
        page.nextKey.shouldBeNull()
        val next = source.genericMangaParse(cannedResponse(html, "https://e-hentai.org/toplist.php?p=1"))
        next.hasNextPage shouldBe true
        next.nextKey shouldBe 3L
    }

    @Test
    fun checkValidPassesNonEmpty() {
        val page = MangasPage(listOf(sManga("/g/1/a/")), false)
        source.checkValid(page) shouldBe page
        harness.ehentai(exh = true).checkValid(page) shouldBe page
    }

    @Test
    fun checkValidEmptyIgneous() {
        val empty = MetadataMangasPage(emptyList(), false, emptyList())
        source.checkValid(empty) shouldBe empty
        val exh = harness.ehentai(exh = true)
        harness.exhPreferences.igneousVal.set("real")
        exh.checkValid(empty) shouldBe empty
        harness.exhPreferences.igneousVal.set("Mystery")
        shouldThrow<IOException> { exh.checkValid(empty) }
    }

    @Test
    fun searchRequestToplist() {
        val filters = source.getFilterList()
        (filters.first { it is ToplistOptions } as ToplistOptions).state = 1
        source.searchRequest(2, "ignored", filters).url.toString() shouldBe
            "https://e-hentai.org/toplist.php?tl=11&p=1"
        source.searchRequest(1, "q", FilterList()).url.toString() shouldBe
            "https://e-hentai.org/?f_apply=Apply%2BFilter&f_search=q"
    }

    @Test
    fun searchRequestForward() {
        val filters = source.getFilterList()
        (filters.first { it is JumpSeekFilter } as JumpSeekFilter).state = "2020"
        val url = source.searchRequest(3, "q", filters).url.toString()
        url shouldBe "https://e-hentai.org/?f_apply=Apply%2BFilter&f_search=q&f_cats=1023&next=3"
        filters.first { it is Filter.Header }.name shouldBe "Note: Will ignore other parameters!"
    }

    @Test
    fun searchRequestReversedJump() {
        val filters = source.getFilterList()
        (filters.first { it is ReverseFilter } as ReverseFilter).state = true
        (filters.first { it is JumpSeekFilter } as JumpSeekFilter).state = "2020"
        val url = source.searchRequest(1, "", filters).url.toString()
        url shouldBe "https://e-hentai.org/?f_apply=Apply%2BFilter&f_search=&f_cats=1023" +
            "&TEH_REVERSE=on&seek=2020&prev=1"
        val blank = source.getFilterList()
        (blank.first { it is JumpSeekFilter } as JumpSeekFilter).state = " "
        source.searchRequest(1, "x", blank).url.toString() shouldBe
            "https://e-hentai.org/?f_apply=Apply%2BFilter&f_search=x&f_cats=1023"
    }
}
