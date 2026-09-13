package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import okhttp3.Request
import okhttp3.Response
import rx.Observable

/**
 * Per-manga layer of an [HttpSource]: details and the chapter list.
 */
public abstract class HttpSourceManga : HttpSourceCatalogue() {
    /**
     * Returns an observable with the updated details for a manga. Normally it's not needed to
     * override this method.
     *
     * @param manga the manga to be updated.
     */
    @Suppress("DEPRECATION")
    @Deprecated("Use the combined suspend API instead", replaceWith = ReplaceWith("getMangaUpdate"))
    override fun fetchMangaDetails(manga: SManga): Observable<SManga> {
        return client.newCall(mangaDetailsRequest(manga))
            .asObservableSuccess()
            .map { response ->
                mangaDetailsParse(response).apply { initialized = true }
            }
    }

    /**
     * Returns the request for the details of a manga. Override only if it's needed to change the
     * url, send different headers or request method like POST.
     *
     * @param manga the manga to be updated.
     */
    @Deprecated(
        message = "The helper functions are inherently limiting and hides the underlying implementation. " +
            "Source developers should make their own implementation according to their needs.",
    )
    public open fun mangaDetailsRequest(manga: SManga): Request = GET(baseUrl + manga.url, headers)

    /**
     * Parses the response from the site and returns the details of a manga.
     *
     * @param response the response from the site.
     */
    @Deprecated(
        message = "The helper functions are inherently limiting and hides the underlying implementation. " +
            "Source developers should make their own implementation according to their needs.",
    )
    protected open fun mangaDetailsParse(response: Response): SManga = throw UnsupportedOperationException()

    /**
     * Returns an observable with the updated chapter list for a manga. Normally it's not needed to
     * override this method.
     *
     * @param manga the manga to look for chapters.
     */
    @Suppress("DEPRECATION")
    @Deprecated("Use the combined suspend API instead", replaceWith = ReplaceWith("getMangaUpdate"))
    override fun fetchChapterList(manga: SManga): Observable<List<SChapter>> {
        return client.newCall(chapterListRequest(manga))
            .asObservableSuccess()
            .map { response ->
                chapterListParse(response)
            }
    }

    /**
     * Returns the request for updating the chapter list. Override only if it's needed to override
     * the url, send different headers or request method like POST.
     *
     * @param manga the manga to look for chapters.
     */
    @Deprecated(
        message = "The helper functions are inherently limiting and hides the underlying implementation. " +
            "Source developers should make their own implementation according to their needs.",
    )
    protected open fun chapterListRequest(manga: SManga): Request = GET(baseUrl + manga.url, headers)

    /**
     * Parses the response from the site and returns a list of chapters.
     *
     * @param response the response from the site.
     */
    @Deprecated(
        message = "The helper functions are inherently limiting and hides the underlying implementation. " +
            "Source developers should make their own implementation according to their needs.",
    )
    protected open fun chapterListParse(response: Response): List<SChapter> = throw UnsupportedOperationException()
}
