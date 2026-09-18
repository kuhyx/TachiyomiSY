package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import net.bytebuddy.ByteBuddy
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy
import net.bytebuddy.implementation.InvocationHandlerAdapter
import net.bytebuddy.matcher.ElementMatchers
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method

private const val PARSED_LEAF: String = "eu.kanade.tachiyomi.source.online.ParsedHttpSource"

/** A manga read from a listing anchor: its href and text. */
internal fun mangaOf(element: Element): SManga = SManga(url = element.attr("href"), title = element.text())

/** A [ParsedHttpSourceDetails] reading anchors out of each listing, with [nextPage] as every next-page selector. */
internal class StubParsedDetailsSource(private val nextPage: String? = "a.next") : ParsedHttpSourceDetails() {
    override val name: String = "Parsed Details"
    override val lang: String = "en"
    override val baseUrl: String = "https://parsed.example"
    override val supportsLatest: Boolean = true

    override fun popularMangaSelector(): String = "div.popular a.entry"

    override fun popularMangaFromElement(element: Element): SManga = mangaOf(element)

    override fun popularMangaNextPageSelector(): String? = nextPage

    override fun searchMangaSelector(): String = "div.search a.entry"

    override fun searchMangaFromElement(element: Element): SManga = mangaOf(element)

    override fun searchMangaNextPageSelector(): String? = nextPage

    override fun latestUpdatesSelector(): String = "div.latest a.entry"

    override fun latestUpdatesFromElement(element: Element): SManga = mangaOf(element)

    override fun latestUpdatesNextPageSelector(): String? = nextPage

    override fun mangaDetailsParse(document: Document): SManga = SManga(url = "/details", title = document.title())
}

/**
 * Answers the abstract members of the generated leaf subclass: identity, the chapter and page
 * selectors and the document parsers. Anything else is a test bug and fails loudly.
 */
private class ParsedLeafHandler : InvocationHandler {
    override fun invoke(proxy: Any, method: Method, args: Array<Any?>?): Any {
        val first = args?.firstOrNull()
        return when (method.name) {
            "getName" -> "Parsed Leaf"
            "getLang" -> "en"
            "getBaseUrl" -> "https://leaf.example"
            "getSupportsLatest" -> true
            "chapterListSelector" -> "li.chapter a"
            "chapterFromElement" -> chapterOf(first as Element)
            "pageListParse" -> pagesOf(first as Document)
            "imageUrlParse" -> (first as Document).select("img").attr("src")
            else -> error("unexpected abstract call on the parsed leaf: ${method.name}")
        }
    }

    private fun chapterOf(element: Element): SChapter = SChapter(name = element.text(), url = element.attr("href"))

    private fun pagesOf(document: Document): List<Page> =
        document.select("img").mapIndexed { index, img -> Page(index = index, url = "", imageUrl = img.attr("src")) }
}

/**
 * A concrete subclass of the deprecated `ParsedHttpSource`, generated at runtime so the test
 * source never names the deprecated class (a deprecation warning is an error in this module).
 * Every abstract member is served by [ParsedLeafHandler].
 */
internal fun parsedLeafSource(): ParsedHttpSourceDetails {
    val leaf = Class.forName(PARSED_LEAF)
    val generated = ByteBuddy()
        .subclass(leaf)
        .method(ElementMatchers.isAbstract())
        .intercept(InvocationHandlerAdapter.of(ParsedLeafHandler()))
        .make()
        .load(leaf.classLoader, ClassLoadingStrategy.Default.WRAPPER)
        .loaded
    return generated.getDeclaredConstructor().newInstance() as ParsedHttpSourceDetails
}
