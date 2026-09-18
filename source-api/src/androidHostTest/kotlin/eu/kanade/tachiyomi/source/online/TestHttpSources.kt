package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SMangaUpdate
import exh.source.DelegatedHttpSource
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import rx.Observable

/** Message on the test overrides of deprecated helpers; the annotation keeps the override warning-free. */
internal const val TEST_OVERRIDE: String = "test override of a deprecated helper"

/** An [HttpSource] that keeps every helper default, so the throwing defaults and the base members are observable. */
internal open class BareHttpSource(
    override val name: String = "Bare Source",
    override val lang: String = "en",
    override val baseUrl: String = "https://bare.example",
) : HttpSource() {
    override val supportsLatest: Boolean = true

    /** The lazily built network layer, exposed for its overridden getters. */
    fun exposedNetwork(): NetworkHelper = network

    /** The header builder, exposed to assert the User-Agent it seeds. */
    fun exposedHeadersBuilder(): Headers.Builder = headersBuilder()

    /** [generateId], exposed to check outdated ids. */
    fun exposedGenerateId(sourceName: String, sourceLang: String, version: Int): Long =
        generateId(sourceName, sourceLang, version)

    /** [SManga.setUrlWithoutDomain] applied to a fresh manga. */
    fun mangaUrlWithoutDomain(url: String): String = SManga.create().apply { setUrlWithoutDomain(url) }.url

    /** [SChapter.setUrlWithoutDomain] applied to a fresh chapter. */
    fun chapterUrlWithoutDomain(url: String): String = SChapter.create().apply { setUrlWithoutDomain(url) }.url
}

/** An [HttpSource] with only the listing request helpers overridden, so their parse defaults are reached. */
internal open class RequestOnlyHttpSource : BareHttpSource(name = "Request Only") {
    @Deprecated(TEST_OVERRIDE)
    override fun popularMangaRequest(page: Int): Request = GET("$baseUrl/popular/$page", headers)

    @Deprecated(TEST_OVERRIDE)
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request =
        GET("$baseUrl/search/$page?q=$query&filters=${filters.size}", headers)

    @Deprecated(TEST_OVERRIDE)
    override fun latestUpdatesRequest(page: Int): Request = GET("$baseUrl/latest/$page", headers)
}

/** An [HttpSource] whose parse helpers echo the response body into the model they return. */
internal class EchoHttpSource : RequestOnlyHttpSource() {
    @Deprecated(TEST_OVERRIDE)
    override fun popularMangaParse(response: Response): MangasPage = pageOf(response, hasNextPage = true)

    @Deprecated(TEST_OVERRIDE)
    override fun searchMangaParse(response: Response): MangasPage = pageOf(response, hasNextPage = false)

    @Deprecated(TEST_OVERRIDE)
    override fun latestUpdatesParse(response: Response): MangasPage = pageOf(response, hasNextPage = true)

    @Deprecated(TEST_OVERRIDE)
    override fun mangaDetailsParse(response: Response): SManga =
        SManga(url = "/details", title = response.body.string())

    @Deprecated(TEST_OVERRIDE)
    override fun chapterListParse(response: Response): List<SChapter> =
        listOf(SChapter(name = response.body.string(), url = "/chapter"))

    @Deprecated(TEST_OVERRIDE)
    override fun pageListParse(response: Response): List<Page> = listOf(Page(index = 0, url = response.body.string()))

    @Deprecated(TEST_OVERRIDE)
    override fun imageUrlParse(response: Response): String = response.body.string()

    private fun pageOf(response: Response, hasNextPage: Boolean): MangasPage =
        MangasPage(listOf(SManga(url = "/listed", title = response.body.string())), hasNextPage)
}

/** A [BareHttpSource] that records the chapter hook and hands out its own filter list, so forwarding is observable. */
internal class RecordingHttpSource : BareHttpSource(name = "Recording") {
    val prepared: MutableList<Pair<SChapter, SManga>> = mutableListOf()
    val filters: FilterList = FilterList()

    @Deprecated(TEST_OVERRIDE)
    override fun prepareNewChapter(chapter: SChapter, manga: SManga) {
        prepared += chapter to manga
    }

    override fun getFilterList(): FilterList = filters
}

/** A [DelegatedHttpSource] with nothing overridden, so every inherited default is the one observed. */
internal class PlainDelegatedSource(delegate: HttpSource) : DelegatedHttpSource(delegate)

/** A [DelegatedHttpSource] whose version, language and clients can be set to force each compatibility branch. */
internal class StubDelegatedSource(
    delegate: HttpSource,
    override val versionId: Int = delegate.versionId,
    override val lang: String = delegate.lang,
    override val baseHttpClient: OkHttpClient? = null,
    private val networkClient: OkHttpClient? = null,
) : DelegatedHttpSource(delegate) {
    override val networkHttpClient: OkHttpClient get() = networkClient ?: super.networkHttpClient
}

/** A [Source] with fixed identity whose suspend API answers its inputs back. */
internal open class StubSource(
    override val id: Long = 7L,
    override val name: String = "Stub Source",
    override val supportsLatest: Boolean = false,
) : Source {
    override suspend fun getPopularManga(page: Int): MangasPage = MangasPage(emptyList(), false)

    override suspend fun getLatestUpdates(page: Int): MangasPage = MangasPage(emptyList(), false)

    override suspend fun getSearchManga(page: Int, query: String, filters: FilterList): MangasPage =
        MangasPage(emptyList(), false)

    override suspend fun getMangaUpdate(
        manga: SManga,
        chapters: List<SChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ): SMangaUpdate = SMangaUpdate(manga, chapters)

    override suspend fun getPageList(chapter: SChapter): List<Page> = emptyList()
}

/** A [CatalogueSource] that keeps every default, so the suspend wrappers hit the throwing Rx defaults. */
internal open class StubCatalogueSource(
    override val id: Long = 9L,
    override val name: String = "Catalogue Source",
    override val lang: String = "en",
    override val supportsLatest: Boolean = false,
) : CatalogueSource

/** A [CatalogueSource] whose Rx API answers canned values, so the suspend wrappers can be observed. */
internal class RxCatalogueSource(
    private val details: SManga,
    private val chapters: List<SChapter>,
    private val pages: List<Page>,
) : StubCatalogueSource(name = "Rx Source") {
    val listing: MangasPage = MangasPage(listOf(details), true)

    @Deprecated(TEST_OVERRIDE)
    override fun fetchPopularManga(page: Int): Observable<MangasPage> = Observable.just(listing)

    @Deprecated(TEST_OVERRIDE)
    override fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> =
        Observable.just(listing)

    @Deprecated(TEST_OVERRIDE)
    override fun fetchLatestUpdates(page: Int): Observable<MangasPage> = Observable.just(listing)

    @Deprecated(TEST_OVERRIDE)
    override fun fetchMangaDetails(manga: SManga): Observable<SManga> = Observable.just(details)

    @Deprecated(TEST_OVERRIDE)
    override fun fetchChapterList(manga: SManga): Observable<List<SChapter>> = Observable.just(chapters)

    @Deprecated(TEST_OVERRIDE)
    override fun fetchPageList(chapter: SChapter): Observable<List<Page>> = Observable.just(pages)
}
