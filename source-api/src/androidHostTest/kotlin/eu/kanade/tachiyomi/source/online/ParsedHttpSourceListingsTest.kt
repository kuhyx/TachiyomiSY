package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.source.model.MangasPage
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

private const val POPULAR_HTML: String =
    "<div class=popular><a class=entry href=/p/1>One</a><a class=entry href=/p/2>Two</a></div><a class=next>n</a>"

private const val SEARCH_HTML: String = "<div class=search><a class=entry href=/s/1>Found</a></div>"

private const val SEARCH_NEXT_HTML: String = "<div class=search></div><a class=next>n</a>"

/** Popular and search parsing of [ParsedHttpSourceListings]: entries, and the next page with and without a selector. */
internal class ParsedHttpSourceListingsTest {
    private val source = StubParsedDetailsSource()
    private val noNextSelector = StubParsedDetailsSource(nextPage = null)

    @Test
    fun popularMapsEachEntry() {
        val page = source.popularParse(POPULAR_HTML)
        page.mangas.map { it.title } shouldBe listOf("One", "Two")
        page.mangas.map { it.url } shouldBe listOf("/p/1", "/p/2")
    }

    @Test
    fun popularNextPageOnMatch() {
        source.popularParse(POPULAR_HTML).hasNextPage shouldBe true
    }

    @Test
    fun popularNoNextPageWithoutMatch() {
        source.popularParse(SEARCH_HTML).hasNextPage shouldBe false
        source.popularParse(SEARCH_HTML).mangas shouldBe emptyList()
    }

    @Test
    fun popularNoNextPageNoSelector() {
        noNextSelector.popularParse(POPULAR_HTML).hasNextPage shouldBe false
        noNextSelector.popularParse(POPULAR_HTML).mangas.size shouldBe 2
    }

    @Test
    fun searchMapsEachEntry() {
        val page = source.searchParse(SEARCH_HTML)
        page.mangas.single().title shouldBe "Found"
        page.mangas.single().url shouldBe "/s/1"
        page.hasNextPage shouldBe false
    }

    @Test
    fun searchNextPageOnMatch() {
        source.searchParse(SEARCH_NEXT_HTML).hasNextPage shouldBe true
    }

    @Test
    fun searchNoNextPageNoSelector() {
        noNextSelector.searchParse(SEARCH_NEXT_HTML).hasNextPage shouldBe false
    }

    private fun StubParsedDetailsSource.popularParse(html: String): MangasPage {
        val args = listOf(cannedResponse(html))
        return invokeDeclared(ParsedHttpSourceListings::class, "popularMangaParse", args) as MangasPage
    }

    private fun StubParsedDetailsSource.searchParse(html: String): MangasPage {
        val args = listOf(cannedResponse(html))
        return invokeDeclared(ParsedHttpSourceListings::class, "searchMangaParse", args) as MangasPage
    }
}
