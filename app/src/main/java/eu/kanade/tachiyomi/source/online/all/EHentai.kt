package eu.kanade.tachiyomi.source.online.all

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.newCachelessCallWithProgress
import eu.kanade.tachiyomi.source.PagePreviewInfo
import eu.kanade.tachiyomi.source.PagePreviewPage
import eu.kanade.tachiyomi.source.PagePreviewSource
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.MetadataMangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SMangaUpdate
import eu.kanade.tachiyomi.source.model.copy
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.MetadataSource
import eu.kanade.tachiyomi.source.online.NamespaceSource
import eu.kanade.tachiyomi.source.online.UrlImportableSource
import eu.kanade.tachiyomi.util.asJsoup
import exh.debug.DebugToggles
import exh.eh.EHentaiUpdateHelper
import exh.eh.GalleryEntry
import exh.log.xLogD
import exh.metadata.MetadataUtil
import exh.metadata.metadata.EHentaiSearchMetadata
import exh.source.ExhPreferences
import exh.source.HELPER_DEPRECATION
import exh.ui.login.EhLoginActivity
import exh.util.UriFilter
import exh.util.nullIfBlank
import exh.util.urlImportSearchManga
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.CacheControl
import okhttp3.CookieJar
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import rx.Observable
import tachiyomi.core.common.util.lang.runAsObservable
import tachiyomi.core.common.util.lang.withIOContext
import uy.kohesive.injekt.injectLazy
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.time.ZoneOffset
import java.time.ZonedDateTime

// Follow-up: Consider gallery updating when doing tabbed browsing (https://github.com/kuhyx/TachiyomiSY/issues/20)
// Positions inside e-hentai's gallery-list markup and its rating sprite.
private const val FIRST_GALLERY_YEAR = 2007
private const val LAST_SEEK_YEAR = 2099

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

    private val exhPreferences: ExhPreferences by injectLazy()
    private val updateHelper: EHentaiUpdateHelper by injectLazy()
    private val galleryListParser = EHentaiGalleryListParser()
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
                .removeHeader("Cookie")
                .addHeader("Cookie", cookiesHeader())
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

    // Parse a list of galleries.
    private fun genericMangaParse(
        response: Response,
    ) = galleryListParser.parse(response.asJsoup()).let { (parsedManga, nextPage) ->
        MetadataMangasPage(
            parsedManga.map { it.manga },
            nextPage != null,
            parsedManga.map { it.metadata },
            nextPage,
        )
    }

    override suspend fun getMangaUpdate(
        manga: SManga,
        chapters: List<SChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ): SMangaUpdate = getMangaUpdate(manga, chapters, fetchDetails, fetchChapters) {}

    suspend fun getMangaUpdate(
        manga: SManga,
        chapters: List<SChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
        throttleFunc: suspend () -> Unit,
    ): SMangaUpdate = supervisorScope {
        val mangaDetails = if (fetchDetails) async { getMangaDetails(manga) } else null
        val chapterDetails = if (fetchChapters) async { getChapterList(manga, throttleFunc) } else null

        SMangaUpdate(mangaDetails?.await() ?: manga, chapterDetails?.await() ?: chapters)
    }

    suspend fun getChapterList(manga: SManga): List<SChapter> = getChapterList(manga) {}

    suspend fun getChapterList(manga: SManga, throttleFunc: suspend () -> Unit): List<SChapter> {
        // Pull all the way to the root gallery
        // We can't do this with RxJava or we run into stack overflows on shit like this:
        //   https://exhentai.org/g/1073061/f9345f1c12/
        var url = manga.url
        var doc: Document

        while (true) {
            val gid = EHentaiSearchMetadata.galleryId(url).toInt()
            val cachedParent = updateHelper.parentLookupTable.get(
                gid,
            )
            if (cachedParent == null) {
                throttleFunc()
                doc = client.newCall(exGet(baseUrl + url)).awaitSuccess().asJsoup()

                val parentLink = doc.select("#gdd .gdt1").find { el ->
                    el.text().lowercase() == "parent:"
                }!!.nextElementSibling()!!.selectFirst("a")?.attr("href")

                if (parentLink != null) {
                    updateHelper.parentLookupTable.put(
                        gid,
                        GalleryEntry(
                            EHentaiSearchMetadata.galleryId(parentLink),
                            EHentaiSearchMetadata.galleryToken(parentLink),
                        ),
                    )
                    url = EHentaiSearchMetadata.normalizeUrl(parentLink)
                } else {
                    break
                }
            } else {
                this@EHentai.xLogD("Parent cache hit: %s!", gid)
                url = EHentaiSearchMetadata.idAndTokenToUrl(
                    cachedParent.gId,
                    cachedParent.gToken,
                )
            }
        }
        val newDisplay = doc.select("#gnd a")
        // Build chapter for root gallery
        val location = doc.location()
        val self = SChapter(
            url = EHentaiSearchMetadata.normalizeUrl(location),
            name = "v1: " + doc.selectFirst("#gn")!!.text(),
            chapterNumber = 1f,
            dateUpload = ZonedDateTime.parse(
                doc.select("#gdd .gdt1").find { el ->
                    el.text().lowercase() == "posted:"
                }!!.nextElementSibling()!!.text(),
                MetadataUtil.EX_DATE_FORMAT.withZone(ZoneOffset.UTC),
            )!!.toInstant().toEpochMilli(),
            scanlator = EHentaiSearchMetadata.galleryId(location),
        )
        // Build and append the rest of the galleries
        return if (DebugToggles.INCLUDE_ONLY_ROOT_WHEN_LOADING_EXH_VERSIONS.enabled) {
            listOf(self)
        } else {
            newDisplay.mapIndexed { index, newGallery ->
                val link = newGallery.attr("href")
                val name = newGallery.text()
                val posted = (newGallery.nextSibling() as TextNode).text().removePrefix(", added ")
                SChapter(
                    url = EHentaiSearchMetadata.normalizeUrl(link),
                    name = "v${index + 2}: $name",
                    chapterNumber = index + 2f,
                    dateUpload = ZonedDateTime.parse(
                        posted,
                        MetadataUtil.EX_DATE_FORMAT.withZone(ZoneOffset.UTC),
                    ).toInstant().toEpochMilli(),
                    scanlator = EHentaiSearchMetadata.galleryId(link),
                )
            }.reversed() + self
        }
    }

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

    private fun parseChapterPage(response: Element) = with(response) {
        select(".gdtm a").map {
            Pair(it.child(0).attr("alt").toInt(), it.attr("href"))
        }.plus(
            select("#gdt a").map {
                Pair(it.child(0).attr("title").removePrefix("Page ").substringBefore(":").toInt(), it.attr("href"))
            },
        ).sortedBy(Pair<Int, String>::first).map { it.second }
    }

    private fun chapterPageRequest(np: String): Request = exGet(url = np, additionalHeaders = headers)

    private fun nextPageUrl(element: Element): String? = element.select("a[onclick=return false]").last()?.let {
        return if (it.text() == ">") it.attr("href") else null
    }

    @Deprecated(HELPER_DEPRECATION)
    override fun popularMangaRequest(page: Int): Request = exGet("$baseUrl/popular")

    private fun <T : MangasPage> T.checkValid(): MangasPage =
        if (exh && mangas.isEmpty() && exhPreferences.igneousVal.get().equals("mystery", true)) {
            throw IOException(
                "Invalid igneous cookie, try re-logging or finding a correct one to input in the login menu",
            )
        } else {
            this
        }

    @Deprecated("Use the non-RxJava API instead", replaceWith = ReplaceWith("getLatestUpdates"))
    override fun fetchLatestUpdates(page: Int): Observable<MangasPage> = runAsObservable { getLatestUpdates(page) }

    override suspend fun getLatestUpdates(page: Int): MangasPage = super<HttpSource>.getLatestUpdates(page).checkValid()

    @Deprecated("Use the non-RxJava API instead", replaceWith = ReplaceWith("getPopularManga"))
    override fun fetchPopularManga(page: Int): Observable<MangasPage> = runAsObservable { getPopularManga(page) }

    override suspend fun getPopularManga(page: Int): MangasPage = super<HttpSource>.getPopularManga(page).checkValid()

    // Support direct URL importing
    @Deprecated("Use the non-RxJava API instead", replaceWith = ReplaceWith("getSearchManga"))
    override fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> =
        runAsObservable { getSearchManga(page, query, filters) }

    override suspend fun getSearchManga(page: Int, query: String, filters: FilterList): MangasPage {
        return urlImportSearchManga(context, query) {
            super<HttpSource>.getSearchManga(page, query, filters).checkValid()
        }
    }

    @Deprecated(HELPER_DEPRECATION)
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val toplist = ToplistOption.entries[filters.firstNotNullOfOrNull { (it as? ToplistOptions)?.state } ?: 0]
        if (toplist != ToplistOption.NONE) {
            val uri = "https://e-hentai.org".toUri().buildUpon()
            uri.appendPath("toplist.php")
            uri.appendQueryParameter("tl", toplist.index.toString())
            uri.appendQueryParameter("p", (page - 1).toString())

            return exGet(url = uri.toString())
        }

        val uri = baseUrl.toUri().buildUpon()
        val isReverseFilterEnabled = filters.any { it is ReverseFilter && it.state }
        val jumpSeekValue = filters.firstNotNullOfOrNull { (it as? JumpSeekFilter)?.state?.nullIfBlank() }

        uri.appendQueryParameter("f_apply", "Apply+Filter")
        uri.appendQueryParameter("f_search", (query + " " + EHentaiQuery.combine(filters)).trim())
        filters.forEach {
            if (it is UriFilter) it.addToUri(uri)
        }
        // Reverse search results on filter
        if (isReverseFilterEnabled) {
            uri.appendQueryParameter(REVERSE_PARAM, "on")
        }
        if (jumpSeekValue != null && page == 1) {
            if (
                MATCH_SEEK_REGEX.matches(jumpSeekValue) ||
                (
                    MATCH_YEAR_REGEX.matches(jumpSeekValue) &&
                        jumpSeekValue.toIntOrNull()?.let {
                            it in FIRST_GALLERY_YEAR..LAST_SEEK_YEAR
                        } == true
                    )
            ) {
                uri.appendQueryParameter("seek", jumpSeekValue)
            } else if (MATCH_JUMP_REGEX.matches(jumpSeekValue)) {
                uri.appendQueryParameter("jump", jumpSeekValue)
            }
        }

        return exGet(
            url = uri.toString(),
            next = if (!isReverseFilterEnabled) page else null,
            prev = if (isReverseFilterEnabled) page else null,
        )
    }

    @Deprecated(HELPER_DEPRECATION)
    override fun latestUpdatesRequest(page: Int) = exGet(baseUrl, page)

    @Deprecated(HELPER_DEPRECATION)
    override fun popularMangaParse(response: Response) = genericMangaParse(response)

    @Deprecated(HELPER_DEPRECATION)
    override fun searchMangaParse(response: Response) = genericMangaParse(response)

    @Deprecated(HELPER_DEPRECATION)
    override fun latestUpdatesParse(response: Response) = genericMangaParse(response)

    private fun exGet(
        url: String,
        next: Int? = null,
        prev: Int? = null,
        additionalHeaders: Headers? = null,
        cacheControl: CacheControl? = null,
    ): Request {
        return GET(
            when {
                next != null && next > 1 -> addParam(url, "next", next.toString())
                prev != null && prev > 0 -> addParam(url, "prev", prev.toString())
                else -> url
            },
            if (additionalHeaders != null) {
                val headers = headers.newBuilder()
                additionalHeaders.toMultimap().forEach { (t, u) ->
                    u.forEach {
                        headers.add(t, it)
                    }
                }
                headers.build()
            } else {
                headers
            },
        ).let {
            if (cacheControl == null) {
                it
            } else {
                it.newBuilder().cacheControl(cacheControl).build()
            }
        }
    }

    /**
     * Returns an observable with the updated details for a manga. Normally it's not needed to
     * override this method.
     *
     * @param manga the manga to be updated.
     */
    @Deprecated("Use the 1.x API instead", replaceWith = ReplaceWith("getMangaDetails"))
    override fun fetchMangaDetails(manga: SManga): Observable<SManga> = runAsObservable { getMangaDetails(manga) }

    // The gallery page, as HttpSource's deprecated `mangaDetailsRequest` default builds it.
    private fun galleryRequest(manga: SManga): Request = GET(baseUrl + manga.url, headers)

    suspend fun getMangaDetails(manga: SManga): SManga {
        val exception = Exception("Async stacktrace")
        val response = client.newCall(galleryRequest(manga)).await()
        if (response.isSuccessful) {
            // Pull to most recent
            val doc = response.asJsoup()
            val newerGallery = doc.select("#gnd a").lastOrNull()
            val pre = if (
                newerGallery != null && DebugToggles.PULL_TO_ROOT_WHEN_LOADING_EXH_MANGA_DETAILS.enabled
            ) {
                val sManga = manga.copy(
                    url = EHentaiSearchMetadata.normalizeUrl(newerGallery.attr("href")),
                )
                client.newCall(galleryRequest(sManga)).awaitSuccess().asJsoup()
            } else {
                doc
            }
            return parseToManga(manga, pre).apply {
                initialized = true
            }
        } else {
            response.close()

            if (response.code == HttpURLConnection.HTTP_NOT_FOUND) {
                throw GalleryNotFoundException(exception)
            } else {
                throw IOException("HTTP error ${response.code}", exception)
            }
        }
    }

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
        throw UnsupportedOperationException("Unused method was called somehow!")

    @Deprecated(HELPER_DEPRECATION)
    override fun pageListParse(response: Response) =
        throw UnsupportedOperationException("Unused method was called somehow!")

    override suspend fun getImageUrl(page: Page): String {
        val imageUrlResponse = client.newCall(GET(page.url, headers)).awaitSuccess()
        return realImageUrlParse(imageUrlResponse, page)
    }

    @Deprecated("Use the non-RxJava API instead", replaceWith = ReplaceWith("getImageUrl"))
    override fun fetchImageUrl(page: Page): Observable<String> = runAsObservable { getImageUrl(page) }

    private fun realImageUrlParse(response: Response, page: Page): String {
        with(response.asJsoup()) {
            val currentImage = getElementById("img")!!.attr("src")
            // Each press of the retry button will choose another server
            select("#loadfail").attr("onclick").nullIfBlank()?.let {
                page.url = addParam(page.url, "nl", it.substring(it.indexOf('\'') + 1 until it.lastIndexOf('\'')))
            }
            if (currentImage == "https://ehgt.org/g/509.gif") {
                throw IOException("Exceeded page quota")
            }
            return currentImage
        }
    }

    @Deprecated(HELPER_DEPRECATION)
    override fun imageUrlParse(response: Response): String {
        throw UnsupportedOperationException("Unused method was called somehow!")
    }

    suspend fun fetchFavorites(): Pair<List<ParsedManga>, List<String>> {
        val favoriteUrl = "$baseUrl/favorites.php"
        val result = mutableListOf<ParsedManga>()
        var page = 1

        var favNames: List<String>? = null

        do {
            val response2 = withIOContext {
                client.newCall(
                    exGet(
                        favoriteUrl,
                        next = page,
                        cacheControl = CacheControl.FORCE_NETWORK,
                    ),
                ).await()
            }
            val doc = response2.asJsoup()

            // Parse favorites
            val parsed = galleryListParser.parse(doc)
            result += parsed.first

            // Parse fav names
            if (favNames == null) {
                favNames = doc.select(".fp:not(.fps)").mapNotNull {
                    it.child(2).text()
                }
            }
            // Next page

            page = parsed.first.lastOrNull()?.manga?.url?.let { EHentaiSearchMetadata.galleryId(it) }?.toInt() ?: 0
        } while (parsed.second != null)

        return Pair(result.toList(), favNames.orEmpty())
    }

    fun spPref() = if (exh) {
        exhPreferences.exhSettingsProfile
    } else {
        exhPreferences.ehSettingsProfile
    }

    private fun rawCookies(sp: Int): Map<String, String> {
        val cookies: MutableMap<String, String> = mutableMapOf()
        if (exhPreferences.enableExhentai.get()) {
            cookies[EhLoginActivity.MEMBER_ID_COOKIE] = exhPreferences.memberIdVal.get()
            cookies[EhLoginActivity.PASS_HASH_COOKIE] = exhPreferences.passHashVal.get()
            cookies[EhLoginActivity.IGNEOUS_COOKIE] = exhPreferences.igneousVal.get()
            cookies["sp"] = sp.toString()

            val sessionKey = exhPreferences.exhSettingsKey.get()
            if (sessionKey.isNotBlank()) {
                cookies["sk"] = sessionKey
            }

            val sessionCookie = exhPreferences.exhSessionCookie.get()
            if (sessionCookie.isNotBlank()) {
                cookies["s"] = sessionCookie
            }

            val hathPerksCookie = exhPreferences.exhHathPerksCookies.get()
            if (hathPerksCookie.isNotBlank()) {
                cookies["hath_perks"] = hathPerksCookie
            }
        }

        // Session-less extended display mode (for users without ExHentai)
        cookies["sl"] = "dm_2"

        // Ignore all content warnings
        cookies["nw"] = "1"

        return cookies
    }

    fun cookiesHeader(sp: Int = spPref().get()) = buildCookies(rawCookies(sp))

    // Headers
    override fun headersBuilder() = super.headersBuilder().add("Cookie", cookiesHeader())

    private fun addParam(url: String, param: String, value: String) = url.toUri()
        .buildUpon()
        .appendQueryParameter(param, value)
        .toString()

    // Filters
    override fun getFilterList(): FilterList {
        return FilterList(
            Filter.Header("Note: Will ignore other parameters!"),
            ToplistOptions(),
            Filter.Separator(),
            AutoCompleteTags(),
            Watched(isEnabled = exhPreferences.exhWatchedListDefaultState.get()),
            GenreGroup(),
            AdvancedGroup(),
            ReverseFilter(),
            JumpSeekFilter(),
        )
    }

    class GalleryNotFoundException(cause: Throwable) : RuntimeException("Gallery not found!", cause)

    // === URL IMPORT STUFF

    override suspend fun mapUrlToMangaUrl(uri: Uri): String? {
        return when (uri.pathSegments.firstOrNull()) {
            "g" -> {
                // Is already gallery page, do nothing
                uri.toString()
            }
            "s" -> {
                // Is page, fetch gallery token and use that
                getGalleryUrlFromPage(uri)
            }
            else -> {
                null
            }
        }
    }

    override fun cleanMangaUrl(url: String): String = EHentaiSearchMetadata.normalizeUrl(super.cleanMangaUrl(url))

    private fun getGalleryUrlFromPage(uri: Uri): String {
        val lastSplit = uri.pathSegments.last().split("-")
        val pageNum = lastSplit.last()
        val gallery = lastSplit.first()
        val pageToken = uri.pathSegments.elementAt(1)

        val json = buildJsonObject {
            put("method", "gtoken")
            put(
                "pagelist",
                buildJsonArray {
                    add(
                        buildJsonArray {
                            add(gallery.toInt())
                            add(pageToken)
                            add(pageNum.toInt())
                        },
                    )
                },
            )
        }

        val outJson = Json.decodeFromString<JsonObject>(
            client.newCall(
                Request.Builder()
                    .url(EH_API_BASE)
                    .post(json.toString().toRequestBody(JSON))
                    .build(),
            ).execute().body.string(),
        )

        val obj = outJson["tokenlist"]!!.jsonArray.first().jsonObject
        return "${uri.scheme}://${uri.host}/g/${obj["gid"]!!.jsonPrimitive.int}/${
            obj["token"]!!.jsonPrimitive.content
        }/"
    }

    override suspend fun getPagePreviewList(
        manga: SManga,
        chapters: List<SChapter>,
        page: Int,
    ): PagePreviewPage {
        val doc = client.newCall(
            exGet(
                (baseUrl + (chapters.lastOrNull()?.url ?: manga.url))
                    .toHttpUrl()
                    .newBuilder()
                    .removeAllQueryParameters("nw")
                    .addQueryParameter("p", (page - 1).toString())
                    .build()
                    .toString(),
            ),
        ).awaitSuccess().asJsoup()

        val body = doc.body()
        val previews = body
            .select("#gdt > div > div")
            .plus(body.select("#gdt > a"))
            .map {
                val preview = parseNormalPreview(it)
                PagePreviewInfo(preview.index, imageUrl = preview.toUrl())
            }
            .ifEmpty {
                body.select("#gdt div a img")
                    .map {
                        PagePreviewInfo(
                            it.attr("alt").toInt(),
                            imageUrl = it.attr("src"),
                        )
                    }
            }

        return PagePreviewPage(
            page = page,
            pagePreviews = previews,
            hasNextPage = doc.select("table.ptt tbody tr td")
                .last()!!
                .hasClass("ptdd")
                .not(),
            pagePreviewPages = doc.select("table.ptt tbody tr td a").asReversed()
                .firstNotNullOfOrNull { it.text().toIntOrNull() },
        )
    }

    override suspend fun fetchPreviewImage(page: PagePreviewInfo, cacheControl: CacheControl?): Response {
        return client.newCachelessCallWithProgress(exGet(page.imageUrl, cacheControl = cacheControl), page)
            .awaitSuccess()
    }

    companion object {

        private val MATCH_YEAR_REGEX = "^\\d{4}\$".toRegex()
        private val MATCH_SEEK_REGEX = """^\d{2,4}-\d{1,2}(-\d{1,2})?""".toRegex()
        private val MATCH_JUMP_REGEX = "^\\d+(\$|d\$|w\$|m\$|y\$|-\$)".toRegex()

        private const val EH_API_BASE = "https://api.e-hentai.org/api.php"
        private val JSON = "application/json; charset=utf-8".toMediaTypeOrNull()!!

        fun buildCookies(cookies: Map<String, String>) = cookies.entries.joinToString(separator = "; ") {
            "${URLEncoder.encode(it.key, "UTF-8")}=${URLEncoder.encode(it.value, "UTF-8")}"
        }
    }
}
