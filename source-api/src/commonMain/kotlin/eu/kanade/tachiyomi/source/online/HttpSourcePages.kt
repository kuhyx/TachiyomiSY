package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.newCachelessCallWithProgress
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import okhttp3.Request
import okhttp3.Response
import rx.Observable
import tachiyomi.core.common.util.lang.awaitSingle

/**
 * Page layer of an [HttpSource]: the page list of a chapter and its images.
 */
public abstract class HttpSourcePages : HttpSourceManga() {
    /**
     * Returns an observable with the page list for a chapter.
     *
     * @param chapter the chapter whose page list has to be fetched.
     */
    @Suppress("DEPRECATION")
    @Deprecated("Use the suspend API instead", ReplaceWith("getPageList"))
    override fun fetchPageList(chapter: SChapter): Observable<List<Page>> {
        return client.newCall(pageListRequest(chapter))
            .asObservableSuccess()
            .map { response ->
                pageListParse(response)
            }
    }

    /**
     * Returns the request for getting the page list. Override only if it's needed to override the
     * url, send different headers or request method like POST.
     *
     * @param chapter the chapter whose page list has to be fetched.
     */
    @Deprecated(
        message = "The helper functions are inherently limiting and hides the underlying implementation. " +
            "Source developers should make their own implementation according to their needs.",
    )
    protected open fun pageListRequest(chapter: SChapter): Request = GET(baseUrl + chapter.url, headers)

    /**
     * Parses the response from the site and returns a list of pages.
     *
     * @param response the response from the site.
     */
    @Deprecated(
        message = "The helper functions are inherently limiting and hides the underlying implementation. " +
            "Source developers should make their own implementation according to their needs.",
    )
    protected open fun pageListParse(response: Response): List<Page> = throw UnsupportedOperationException()

    /**
     * Returns an observable with the page containing the source url of the image. If there's any
     * error, it will return null instead of throwing an exception.
     *
     * @param page the page whose source image has to be fetched.
     */
    @Suppress("DEPRECATION")
    @Deprecated("Use the suspend API instead", ReplaceWith("getImageUrl"))
    public open fun fetchImageUrl(page: Page): Observable<String> {
        return client.newCall(imageUrlRequest(page))
            .asObservableSuccess()
            .map { imageUrlParse(it) }
    }

    /**
     * Returns the image url for the provided [page]. The function is only called if [Page.imageUrl] is null.
     *
     * @since tachiyomix 1.6
     * @param page the page whose source image has to be fetched.
     */
    @Suppress("DEPRECATION")
    public open suspend fun getImageUrl(page: Page): String = fetchImageUrl(page).awaitSingle()

    /**
     * Returns the request for getting the url to the source image. Override only if it's needed to
     * override the url, send different headers or request method like POST.
     *
     * @param page the chapter whose page list has to be fetched
     */
    @Deprecated(
        message = "The helper functions are inherently limiting and hides the underlying implementation. " +
            "Source developers should make their own implementation according to their needs.",
    )
    protected open fun imageUrlRequest(page: Page): Request = GET(page.url, headers)

    /**
     * Parses the response from the site and returns the absolute url to the source image.
     *
     * @param response the response from the site.
     */
    @Deprecated(
        message = "The helper functions are inherently limiting and hides the underlying implementation. " +
            "Source developers should make their own implementation according to their needs.",
    )
    protected open fun imageUrlParse(response: Response): String = throw UnsupportedOperationException()

    /* SY --> protected <-- SY */

    /** Downloads the image of [page], resuming after [existingSize] bytes when possible. */
    public open suspend fun getImage(page: Page, existingSize: Long = 0L): Response {
        return client.newCachelessCallWithProgress(imageRequest(page), page, existingSize)
            .awaitSuccess()
    }

    /**
     * Returns the request for getting the source image. Override only if it's needed to override
     * the url, send different headers or request method like POST.
     *
     * @param page the chapter whose page list has to be fetched
     */
    protected open fun imageRequest(page: Page): Request = GET(page.imageUrl!!, headers)
}
