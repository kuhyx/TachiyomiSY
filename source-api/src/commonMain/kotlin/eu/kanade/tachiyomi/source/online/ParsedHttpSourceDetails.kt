package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * Jsoup layer of [ParsedHttpSource] for the latest-updates listing and the manga details page.
 */
public abstract class ParsedHttpSourceDetails : ParsedHttpSourceListings() {
    /**
     * Parses the response from the site and returns a [MangasPage] object.
     *
     * @param response the response from the site.
     */
    @Deprecated(
        message = "The helper functions are inherently limiting and hides the underlying implementation. " +
            "Source developers should make their own implementation according to their needs.",
    )
    override fun latestUpdatesParse(response: Response): MangasPage {
        val document = response.asJsoup()

        val mangas = document.select(latestUpdatesSelector()).map { element ->
            latestUpdatesFromElement(element)
        }

        val hasNextPage = latestUpdatesNextPageSelector()?.let { selector ->
            document.select(selector).first()
        } != null

        return MangasPage(mangas, hasNextPage)
    }

    /**
     * Returns the Jsoup selector that returns a list of [Element] corresponding to each manga.
     */
    protected abstract fun latestUpdatesSelector(): String

    /**
     * Returns a manga from the given [element]. Most sites only show the title and the url, it's
     * totally fine to fill only those two values.
     *
     * @param element an element obtained from [latestUpdatesSelector].
     */
    protected abstract fun latestUpdatesFromElement(element: Element): SManga

    /**
     * Returns the Jsoup selector that returns the <a> tag linking to the next page, or null if
     * there's no next page.
     */
    protected abstract fun latestUpdatesNextPageSelector(): String?

    /**
     * Parses the response from the site and returns the details of a manga.
     *
     * @param response the response from the site.
     */
    @Deprecated(
        message = "The helper functions are inherently limiting and hides the underlying implementation. " +
            "Source developers should make their own implementation according to their needs.",
    )
    override fun mangaDetailsParse(response: Response): SManga = mangaDetailsParse(response.asJsoup())

    /**
     * Returns the details of the manga from the given [document].
     *
     * @param document the parsed document.
     */
    protected abstract fun mangaDetailsParse(document: Document): SManga
}
