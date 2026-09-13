package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * A simple implementation for sources from a website using Jsoup, an HTML parser.
 */
@Deprecated(
    message = "In most cases sources only require a subset of the methods from this class. " +
        "Source developers should make their own implementation according to their needs.",
)
public abstract class ParsedHttpSource : ParsedHttpSourceDetails() {
    /**
     * Parses the response from the site and returns a list of chapters.
     *
     * @param response the response from the site.
     */
    @Deprecated(
        message = "The helper functions are inherently limiting and hides the underlying implementation. " +
            "Source developers should make their own implementation according to their needs.",
    )
    override fun chapterListParse(response: Response): List<SChapter> {
        val document = response.asJsoup()
        return document.select(chapterListSelector()).map { chapterFromElement(it) }
    }

    /**
     * Returns the Jsoup selector that returns a list of [Element] corresponding to each chapter.
     */
    protected abstract fun chapterListSelector(): String

    /**
     * Returns a chapter from the given element.
     *
     * @param element an element obtained from [chapterListSelector].
     */
    protected abstract fun chapterFromElement(element: Element): SChapter

    /**
     * Parses the response from the site and returns the page list.
     *
     * @param response the response from the site.
     */
    @Deprecated(
        message = "The helper functions are inherently limiting and hides the underlying implementation. " +
            "Source developers should make their own implementation according to their needs.",
    )
    override fun pageListParse(response: Response): List<Page> = pageListParse(response.asJsoup())

    /**
     * Returns a page list from the given document.
     *
     * @param document the parsed document.
     */
    protected abstract fun pageListParse(document: Document): List<Page>

    /**
     * Parse the response from the site and returns the absolute url to the source image.
     *
     * @param response the response from the site.
     */
    @Deprecated(
        message = "The helper functions are inherently limiting and hides the underlying implementation. " +
            "Source developers should make their own implementation according to their needs.",
    )
    override fun imageUrlParse(response: Response): String = imageUrlParse(response.asJsoup())

    /**
     * Returns the absolute url to the source image from the document.
     *
     * @param document the parsed document.
     */
    protected abstract fun imageUrlParse(document: Document): String
}
