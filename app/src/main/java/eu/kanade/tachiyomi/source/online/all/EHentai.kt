package eu.kanade.tachiyomi.source.online.all

import android.content.Context
import android.net.Uri
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.newCachelessCallWithProgress
import eu.kanade.tachiyomi.source.PagePreviewInfo
import eu.kanade.tachiyomi.source.PagePreviewPage
import eu.kanade.tachiyomi.source.PagePreviewSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SMangaUpdate
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.MetadataSource
import eu.kanade.tachiyomi.source.online.NamespaceSource
import eu.kanade.tachiyomi.source.online.UrlImportableSource
import eu.kanade.tachiyomi.util.asJsoup
import exh.eh.EHentaiUpdateHelper
import exh.metadata.metadata.EHentaiSearchMetadata
import exh.source.ExhPreferences
import exh.source.HELPER_DEPRECATION
import exh.util.urlImportSearchManga
import kotlinx.serialization.json.add
import okhttp3.CacheControl
import okhttp3.CookieJar
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import rx.Observable
import tachiyomi.core.common.util.lang.runAsObservable
import uy.kohesive.injekt.injectLazy
import java.net.URLEncoder

private const val UNUSED_METHOD_WAS_CALLED_SOMEHOW = "Unused method was called somehow!"
private const val COOKIE = "Cookie"

internal class EHentai(
    override val id: Long,
    val exh: Boolean,
    val context: Context,
) : HttpSource(),
    MetadataSource<EHentaiSearchMetadata, Document>,
    UrlImportableSource,
    NamespaceSource,
    PagePreviewSource {
    override val metaClass = EHentaiSearchMetadata::class

    private val domain: String
        get() = if (exh) {
            "exhentai.org"
        } else {
            "e-hentai.org"
        }

    override val baseUrl: String
        get() = "https://$domain"

    override val lang = "all"
    override val supportsLatest = true

    internal val exhPreferences: ExhPreferences by injectLazy()
    internal val updateHelper: EHentaiUpdateHelper by injectLazy()
    internal val galleryListParser = EHentaiGalleryListParser()
    private val metadataParser = EHentaiMetadataParser(exh)

    /**
     * Gallery list entry.
     */
    data class ParsedManga(val fav: Int, val manga: SManga, val metadata: EHentaiSearchMetadata)

    override val client = network.client.newBuilder()
        .cookieJar(CookieJar.NO_COOKIES)
        .addInterceptor { chain ->
            val newReq = chain
                .request()
                .newBuilder()
                .removeHeader(COOKIE)
                .addHeader(COOKIE, cookiesHeader())
                .build()

            chain.proceed(newReq)
        }
        .addInterceptor(ThumbnailPreviewInterceptor())
        .build()

    override val name = if (exh) {
        "ExHentai"
    } else {
        "E-Hentai"
    }

    override val matchingHosts: List<String> = if (exh) {
        listOf(
            "exhentai.org",
        )
    } else {
        listOf(
            "g.e-hentai.org",
            "e-hentai.org",
        )
    }

    override suspend fun getMangaUpdate(
        manga: SManga,
        chapters: List<SChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ): SMangaUpdate = getMangaUpdate(manga, chapters, fetchDetails, fetchChapters) {}

    @Deprecated("Use the 1.x API instead", replaceWith = ReplaceWith("getChapterList"))
    override fun fetchChapterList(manga: SManga): Observable<List<SChapter>> = runAsObservable { getChapterList(manga) }

    @Deprecated("Use the 1.x API instead", replaceWith = ReplaceWith("getPageList"))
    override fun fetchPageList(chapter: SChapter): Observable<List<Page>> = runAsObservable { getPageList(chapter) }

    override suspend fun getPageList(chapter: SChapter): List<Page> {
        val urls = mutableListOf<String>()
        var nextUrl: String? = baseUrl + chapter.url
        while (nextUrl != null) {
            val jsoup = client.newCall(chapterPageRequest(nextUrl)).awaitSuccess().asJsoup()
            urls += parseChapterPage(jsoup)
            nextUrl = nextPageUrl(jsoup)
        }
        return urls.mapIndexed { i, s -> Page(i, s) }
    }

    @Deprecated(HELPER_DEPRECATION)
    override fun popularMangaRequest(page: Int): Request = exGet("$baseUrl/popular")

    @Deprecated("Use the non-RxJava API instead", replaceWith = ReplaceWith("getLatestUpdates"))
    override fun fetchLatestUpdates(page: Int): Observable<MangasPage> = runAsObservable { getLatestUpdates(page) }

    override suspend fun getLatestUpdates(page: Int): MangasPage = checkValid(super<HttpSource>.getLatestUpdates(page))

    @Deprecated("Use the non-RxJava API instead", replaceWith = ReplaceWith("getPopularManga"))
    override fun fetchPopularManga(page: Int): Observable<MangasPage> = runAsObservable { getPopularManga(page) }

    override suspend fun getPopularManga(page: Int): MangasPage = checkValid(super<HttpSource>.getPopularManga(page))

    // Support direct URL importing
    @Deprecated("Use the non-RxJava API instead", replaceWith = ReplaceWith("getSearchManga"))
    override fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> =
        runAsObservable { getSearchManga(page, query, filters) }

    override suspend fun getSearchManga(page: Int, query: String, filters: FilterList): MangasPage {
        return urlImportSearchManga(context, query) {
            checkValid(super<HttpSource>.getSearchManga(page, query, filters))
        }
    }

    @Deprecated(HELPER_DEPRECATION)
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request =
        searchRequest(page, query, filters)

    @Deprecated(HELPER_DEPRECATION)
    override fun latestUpdatesRequest(page: Int) = exGet(baseUrl, page)

    @Deprecated(HELPER_DEPRECATION)
    override fun popularMangaParse(response: Response) = genericMangaParse(response)

    @Deprecated(HELPER_DEPRECATION)
    override fun searchMangaParse(response: Response) = genericMangaParse(response)

    @Deprecated(HELPER_DEPRECATION)
    override fun latestUpdatesParse(response: Response) = genericMangaParse(response)

    /**
     * Returns an observable with the updated details for a manga. Normally it's not needed to
     * override this method.
     *
     * @param manga the manga to be updated.
     */
    @Deprecated("Use the 1.x API instead", replaceWith = ReplaceWith("getMangaDetails"))
    override fun fetchMangaDetails(manga: SManga): Observable<SManga> = runAsObservable { getMangaDetails(manga) }

    /**
     * Parse gallery page to metadata model.
     */
    @Deprecated(HELPER_DEPRECATION)
    override fun mangaDetailsParse(response: Response) = throw UnsupportedOperationException()

    override fun newMetaInstance() = EHentaiSearchMetadata()

    override suspend fun parseIntoMetadata(metadata: EHentaiSearchMetadata, input: Document) {
        metadataParser.parseInto(metadata, input)
    }

    @Deprecated(HELPER_DEPRECATION)
    override fun chapterListParse(response: Response) =
        throw UnsupportedOperationException(UNUSED_METHOD_WAS_CALLED_SOMEHOW)

    @Deprecated(HELPER_DEPRECATION)
    override fun pageListParse(response: Response) =
        throw UnsupportedOperationException(UNUSED_METHOD_WAS_CALLED_SOMEHOW)

    override suspend fun getImageUrl(page: Page): String {
        val imageUrlResponse = client.newCall(GET(page.url, headers)).awaitSuccess()
        return realImageUrlParse(imageUrlResponse, page)
    }

    @Deprecated("Use the non-RxJava API instead", replaceWith = ReplaceWith("getImageUrl"))
    override fun fetchImageUrl(page: Page): Observable<String> = runAsObservable { getImageUrl(page) }

    @Deprecated(HELPER_DEPRECATION)
    override fun imageUrlParse(response: Response): String {
        throw UnsupportedOperationException(UNUSED_METHOD_WAS_CALLED_SOMEHOW)
    }

    // Headers
    override fun headersBuilder() = super.headersBuilder().add(COOKIE, cookiesHeader())

    // Filters
    override fun getFilterList(): FilterList = filterList()

    class GalleryNotFoundException(cause: Throwable) : RuntimeException("Gallery not found!", cause)

    // === URL IMPORT STUFF

    override suspend fun mapUrlToMangaUrl(uri: Uri): String? = mangaUrlFromUri(uri)

    override fun cleanMangaUrl(url: String): String = EHentaiSearchMetadata.normalizeUrl(super.cleanMangaUrl(url))

    override suspend fun getPagePreviewList(manga: SManga, chapters: List<SChapter>, page: Int): PagePreviewPage =
        pagePreviewList(manga, chapters, page)

    override suspend fun fetchPreviewImage(page: PagePreviewInfo, cacheControl: CacheControl?): Response {
        return client.newCachelessCallWithProgress(exGet(page.imageUrl, cacheControl = cacheControl), page)
            .awaitSuccess()
    }

    companion object {

        internal const val EH_API_BASE = "https://api.e-hentai.org/api.php"
        internal val JSON = "application/json; charset=utf-8".toMediaTypeOrNull()!!

        fun buildCookies(cookies: Map<String, String>) = cookies.entries.joinToString(separator = "; ") {
            "${URLEncoder.encode(it.key, "UTF-8")}=${URLEncoder.encode(it.value, "UTF-8")}"
        }
    }
}
