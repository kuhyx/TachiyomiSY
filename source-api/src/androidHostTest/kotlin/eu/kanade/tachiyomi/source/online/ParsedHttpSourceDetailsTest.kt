package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private const val LATEST_HTML: String =
    "<div class=latest><a class=entry href=/l/1>New</a><a class=entry href=/l/2>Newer</a></div><a class=next>n</a>"

/** Latest-updates parsing and the details document hand-off of [ParsedHttpSourceDetails]. */
internal class ParsedHttpSourceDetailsTest {
    private val harness = SourceHarness()
    private val source = StubParsedDetailsSource()
    private val noNextSelector = StubParsedDetailsSource(nextPage = null)

    @BeforeEach
    fun setUp() = harness.install()

    @AfterEach
    fun tearDown() = harness.uninstall()

    @Test
    fun latestMapsEachEntry() {
        val page = source.latestParse(LATEST_HTML)
        page.mangas.map { it.title } shouldBe listOf("New", "Newer")
        page.mangas.map { it.url } shouldBe listOf("/l/1", "/l/2")
        page.hasNextPage shouldBe true
    }

    @Test
    fun latestNoNextPageWithoutMatch() {
        val page = source.latestParse("<div class=latest></div>")
        page.mangas shouldBe emptyList()
        page.hasNextPage shouldBe false
    }

    @Test
    fun latestNoNextPageNoSelector() {
        noNextSelector.latestParse(LATEST_HTML).hasNextPage shouldBe false
    }

    @Test
    fun detailsParseUsesDocument() = runTest {
        harness.server.body = "<html><head><title>Parsed title</title></head><body></body></html>"
        val manga = SManga(url = "/m/1", title = "old")
        val update =
            source.getMangaUpdate(manga = manga, chapters = emptyList(), fetchDetails = true, fetchChapters = false)
        update.manga.title shouldBe "Parsed title"
        update.manga.initialized shouldBe true
        harness.server.requests.single().url.toString() shouldBe "https://parsed.example/m/1"
    }

    private fun StubParsedDetailsSource.latestParse(html: String): MangasPage {
        val args = listOf(cannedResponse(html))
        return invokeDeclared(ParsedHttpSourceDetails::class, "latestUpdatesParse", args) as MangasPage
    }
}
