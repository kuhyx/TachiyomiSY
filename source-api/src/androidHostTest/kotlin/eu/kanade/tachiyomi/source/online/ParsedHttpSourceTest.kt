package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private const val CHAPTERS_HTML: String =
    "<ul><li class=chapter><a href=/c/2>Ch 2</a></li><li class=chapter><a href=/c/1>Ch 1</a></li></ul>"

private const val PAGES_HTML: String = "<div><img src=https://img.example/1.png><img src=https://img.example/2.png>"

/** The three Jsoup hand-offs of the deprecated `ParsedHttpSource` leaf: chapters, pages and the image url. */
internal class ParsedHttpSourceTest {
    private val harness = SourceHarness()
    private val source = parsedLeafSource()
    private val manga = SManga(url = "/m/1", title = "t")
    private val chapter = SChapter(name = "c", url = "/c/1")

    @BeforeEach
    fun setUp() = harness.install()

    @AfterEach
    fun tearDown() = harness.uninstall()

    @Test
    fun generatedLeafHasIdentity() {
        source.name shouldBe "Parsed Leaf"
        source.lang shouldBe "en"
        source.baseUrl shouldBe "https://leaf.example"
        source.supportsLatest shouldBe true
        source.toString() shouldBe "Parsed Leaf (EN)"
    }

    @Test
    fun chapterListParsesEachElement() = runTest {
        harness.server.body = CHAPTERS_HTML
        val update =
            source.getMangaUpdate(manga = manga, chapters = emptyList(), fetchDetails = false, fetchChapters = true)
        update.chapters.map { it.name } shouldBe listOf("Ch 2", "Ch 1")
        update.chapters.map { it.url } shouldBe listOf("/c/2", "/c/1")
        harness.server.requests.single().url.toString() shouldBe "https://leaf.example/m/1"
    }

    @Test
    fun chapterListEmptyWithoutMatches() = runTest {
        harness.server.body = "<ul></ul>"
        val update =
            source.getMangaUpdate(manga = manga, chapters = emptyList(), fetchDetails = false, fetchChapters = true)
        update.chapters shouldBe emptyList()
    }

    @Test
    fun pageListParsesDocument() = runTest {
        harness.server.body = PAGES_HTML
        val pages = source.getPageList(chapter)
        pages.map { it.imageUrl } shouldBe listOf("https://img.example/1.png", "https://img.example/2.png")
        pages.map { it.index } shouldBe listOf(0, 1)
        harness.server.requests.single().url.toString() shouldBe "https://leaf.example/c/1"
    }

    @Test
    fun imageUrlParsesDocument() = runTest {
        harness.server.body = "<img src=https://img.example/only.png>"
        source.getImageUrl(Page(index = 0, url = "https://leaf.example/p/1")) shouldBe "https://img.example/only.png"
        harness.server.requests.single().url.toString() shouldBe "https://leaf.example/p/1"
    }
}
