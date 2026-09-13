package exh.source

import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SMangaUpdate
import eu.kanade.tachiyomi.source.online.HttpSource
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import rx.Observable

/** SY: an in-app replacement for an extension source; every request goes through the [delegate].
 *
 * @property delegate the extension source being wrapped. */
@Suppress("OverridingDeprecatedMember", "DEPRECATION")
public abstract class DelegatedHttpSource(public val delegate: HttpSource) : HttpSource() {
    override val lang: String get() = delegate.lang

    override val baseUrl: String get() = delegate.baseUrl

    override val headers: Headers get() = delegate.headers

    override val supportsLatest: Boolean get() = delegate.supportsLatest

    final override val name: String get() = delegate.name

    override val id: Long get() = delegate.id

    final override val client: OkHttpClient get() = delegate.client

    /** The client the delegate should use instead of its own; never call `super.client` when overriding. */
    public open val baseHttpClient: OkHttpClient? = null

    /** The client used for the delegate's network requests. */
    public open val networkHttpClient: OkHttpClient get() = network.client

    init {
        delegate.bindDelegate(this)
    }

    @Deprecated(HELPER_DEPRECATION)
    override fun popularMangaRequest(page: Int): Request =
        throw UnsupportedOperationException(NEVER_CALLED)

    @Deprecated(HELPER_DEPRECATION)
    override fun popularMangaParse(response: Response): MangasPage =
        throw UnsupportedOperationException(NEVER_CALLED)

    @Deprecated(HELPER_DEPRECATION)
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request =
        throw UnsupportedOperationException(NEVER_CALLED)

    @Deprecated(HELPER_DEPRECATION)
    override fun searchMangaParse(response: Response): MangasPage =
        throw UnsupportedOperationException(NEVER_CALLED)

    @Deprecated(HELPER_DEPRECATION)
    override fun latestUpdatesRequest(page: Int): Request =
        throw UnsupportedOperationException(NEVER_CALLED)

    @Deprecated(HELPER_DEPRECATION)
    override fun latestUpdatesParse(response: Response): MangasPage =
        throw UnsupportedOperationException(NEVER_CALLED)

    @Deprecated(HELPER_DEPRECATION)
    override fun mangaDetailsParse(response: Response): SManga =
        throw UnsupportedOperationException(NEVER_CALLED)

    @Deprecated(HELPER_DEPRECATION)
    override fun chapterListParse(response: Response): List<SChapter> =
        throw UnsupportedOperationException(NEVER_CALLED)

    @Deprecated(HELPER_DEPRECATION)
    override fun pageListParse(response: Response): List<Page> =
        throw UnsupportedOperationException(NEVER_CALLED)

    @Deprecated(HELPER_DEPRECATION)
    override fun imageUrlParse(response: Response): String =
        throw UnsupportedOperationException(NEVER_CALLED)

    override fun getHomeUrl(): String = delegate.getHomeUrl()

    // ===> OPTIONAL FIELDS

    override fun toString(): String = delegate.toString()

    @Deprecated("Use the non-RxJava API instead", replaceWith = ReplaceWith("getPopularManga"))
    override fun fetchPopularManga(page: Int): Observable<MangasPage> {
        ensureDelegateCompatible()
        return delegate.fetchPopularManga(page)
    }

    override suspend fun getPopularManga(page: Int): MangasPage {
        ensureDelegateCompatible()
        return delegate.getPopularManga(page)
    }

    @Deprecated("Use the non-RxJava API instead", replaceWith = ReplaceWith("getSearchManga"))
    override fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> {
        ensureDelegateCompatible()
        return delegate.fetchSearchManga(page, query, filters)
    }

    override suspend fun getSearchManga(page: Int, query: String, filters: FilterList): MangasPage {
        ensureDelegateCompatible()
        return delegate.getSearchManga(page, query, filters)
    }

    @Deprecated("Use the non-RxJava API instead", replaceWith = ReplaceWith("getLatestUpdates"))
    override fun fetchLatestUpdates(page: Int): Observable<MangasPage> {
        ensureDelegateCompatible()
        return delegate.fetchLatestUpdates(page)
    }

    override suspend fun getLatestUpdates(page: Int): MangasPage {
        ensureDelegateCompatible()
        return delegate.getLatestUpdates(page)
    }

    @Deprecated("Use the 1.x API instead", replaceWith = ReplaceWith("getMangaDetails"))
    override fun fetchMangaDetails(manga: SManga): Observable<SManga> {
        ensureDelegateCompatible()
        return delegate.fetchMangaDetails(manga)
    }

    override suspend fun getMangaUpdate(
        manga: SManga,
        chapters: List<SChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ): SMangaUpdate {
        ensureDelegateCompatible()
        return delegate.getMangaUpdate(manga, chapters, fetchDetails, fetchChapters)
    }

    @Deprecated(HELPER_DEPRECATION)
    override fun mangaDetailsRequest(manga: SManga): Request {
        ensureDelegateCompatible()
        return delegate.mangaDetailsRequest(manga)
    }

    @Deprecated("Use the 1.x API instead", replaceWith = ReplaceWith("getChapterList"))
    override fun fetchChapterList(manga: SManga): Observable<List<SChapter>> {
        ensureDelegateCompatible()
        return delegate.fetchChapterList(manga)
    }

    @Deprecated("Use the 1.x API instead", replaceWith = ReplaceWith("getPageList"))
    override fun fetchPageList(chapter: SChapter): Observable<List<Page>> {
        ensureDelegateCompatible()
        return delegate.fetchPageList(chapter)
    }

    override suspend fun getPageList(chapter: SChapter): List<Page> {
        ensureDelegateCompatible()
        return delegate.getPageList(chapter)
    }

    @Deprecated("Use the non-RxJava API instead", replaceWith = ReplaceWith("getImageUrl"))
    override fun fetchImageUrl(page: Page): Observable<String> {
        ensureDelegateCompatible()
        return delegate.fetchImageUrl(page)
    }

    override suspend fun getImageUrl(page: Page): String {
        ensureDelegateCompatible()
        return delegate.getImageUrl(page)
    }

    override suspend fun getImage(page: Page, existingSize: Long): Response {
        ensureDelegateCompatible()
        return delegate.getImage(page, existingSize)
    }

    override fun getMangaUrl(manga: SManga): String {
        ensureDelegateCompatible()
        return delegate.getMangaUrl(manga)
    }

    override fun getChapterUrl(chapter: SChapter): String {
        ensureDelegateCompatible()
        return delegate.getChapterUrl(chapter)
    }

    @Deprecated("All modifications should be done when constructing the chapter")
    override fun prepareNewChapter(chapter: SChapter, manga: SManga) {
        ensureDelegateCompatible()
        return delegate.prepareNewChapter(chapter, manga)
    }

    override fun getFilterList(): FilterList = delegate.getFilterList()

    protected open fun ensureDelegateCompatible() {
        if (versionId != delegate.versionId || lang != delegate.lang) {
            throw IncompatibleDelegateException(
                "Delegate source is not compatible (" +
                    "versionId: $versionId <=> ${delegate.versionId}, lang: $lang <=> ${delegate.lang}" +
                    ")!",
            )
        }
    }

    /** Thrown when the delegate's version or language differs from this source's. */

    public class IncompatibleDelegateException(message: String) : RuntimeException(message)
}
