package eu.kanade.tachiyomi.source.online.all

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import eu.kanade.domain.track.service.TrackPreferences
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.data.track.mdlist.MdList
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.MetadataMangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.FollowsSource
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.LoginSource
import eu.kanade.tachiyomi.source.online.MetadataSource
import eu.kanade.tachiyomi.source.online.NamespaceSource
import eu.kanade.tachiyomi.source.online.RandomMangaSource
import eu.kanade.tachiyomi.source.online.UrlImportableSource
import exh.md.dto.MangaDto
import exh.md.dto.StatisticsMangaDto
import exh.md.handlers.ApiMangaParser
import exh.md.handlers.AzukiHandler
import exh.md.handlers.BilibiliHandler
import exh.md.handlers.ComikeyHandler
import exh.md.handlers.FollowsHandler
import exh.md.handlers.MangaHandler
import exh.md.handlers.MangaHotHandler
import exh.md.handlers.MangaPlusHandler
import exh.md.handlers.NamicomiHandler
import exh.md.handlers.PageHandler
import exh.md.handlers.SimilarHandler
import exh.md.network.MangaDexLoginHelper
import exh.md.service.MangaDexAuthService
import exh.md.service.MangaDexService
import exh.md.service.SimilarService
import exh.md.utils.FollowStatus
import exh.md.utils.MdApi
import exh.md.utils.MdLang
import exh.md.utils.MdUtil
import exh.metadata.metadata.MangaDexSearchMetadata
import exh.source.DelegatedHttpSource
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Response
import rx.Observable
import tachiyomi.core.common.util.lang.runAsObservable
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import kotlin.reflect.KClass

internal class MangaDex(delegate: HttpSource, val context: Context) :
    DelegatedHttpSource(delegate),
    MetadataSource<MangaDexSearchMetadata, Triple<MangaDto, List<String>, StatisticsMangaDto>>,
    UrlImportableSource,
    FollowsSource,
    LoginSource,
    RandomMangaSource,
    NamespaceSource {
    override val lang: String = delegate.lang

    private val mdLang by lazy {
        MdLang.fromExt(lang) ?: MdLang.ENGLISH
    }

    override val matchingHosts: List<String> = listOf("mangadex.org", "www.mangadex.org")

    val trackPreferences: TrackPreferences by injectLazy()
    val mdList: MdList by lazy { Injekt.get<TrackerManager>().mdList }

    private val sourcePreferences: SharedPreferences by lazy {
        context.getSharedPreferences("source_$id", 0x0000)
    }

    private val loginHelper = MangaDexLoginHelper(network.client, trackPreferences, mdList, mdList.interceptor)

    override val headers: Headers = delegate.headers.newBuilder()
        .removeAll("User-Agent")
        .add("User-Agent", "TachiyomiSY v${BuildConfig.VERSION_NAME} (${BuildConfig.APPLICATION_ID})")
        .build()

    override val baseHttpClient: OkHttpClient = delegate.client.newBuilder()
        .addInterceptor(mdList.interceptor)
        .build()

    private val mangadexService by lazy {
        MangaDexService(client, headers)
    }

    private val mangadexAuthService by lazy {
        MangaDexAuthService(baseHttpClient, headers)
    }

    private val similarService by lazy {
        SimilarService(client)
    }

    private val apiMangaParser by lazy {
        ApiMangaParser(mdLang.lang)
    }

    private val followsHandler by lazy {
        FollowsHandler(mdLang.lang, mangadexAuthService)
    }

    private val mangaHandler by lazy {
        MangaHandler(mdLang.lang, mangadexService, apiMangaParser)
    }

    private val similarHandler by lazy {
        SimilarHandler(mdLang.lang, mangadexService, similarService)
    }

    private val mangaPlusHandler by lazy {
        MangaPlusHandler(network.client)
    }

    private val comikeyHandler by lazy {
        ComikeyHandler(network.client, network.defaultUserAgentProvider())
    }

    private val bilibiliHandler by lazy {
        BilibiliHandler(network.client)
    }

    private val azukHandler by lazy {
        AzukiHandler(network.client, network.defaultUserAgentProvider())
    }

    private val mangaHotHandler by lazy {
        MangaHotHandler(network.client, network.defaultUserAgentProvider())
    }

    private val namicomiHandler by lazy {
        NamicomiHandler(network.client, network.defaultUserAgentProvider())
    }

    private val pageHandler by lazy {
        PageHandler(
            mangadexService,
            PageHandler.ExternalHandlers(
                mangaPlus = mangaPlusHandler,
                comikey = comikeyHandler,
                bilibili = bilibiliHandler,
                azuki = azukHandler,
                mangaHot = mangaHotHandler,
                namicomi = namicomiHandler,
            ),
        )
    }

    // MetadataSource methods
    override val metaClass: KClass<MangaDexSearchMetadata> = MangaDexSearchMetadata::class

    // LoginSource methods
    override val requiresLogin: Boolean = false

    override val twoFactorAuth = LoginSource.AuthSupport.NOT_SUPPORTED

    private fun dataSaver() = sourcePreferences.getBoolean(getDataSaverPreferenceKey(mdLang.lang), false)
    private fun usePort443Only() = sourcePreferences.getBoolean(getStandardHttpsPreferenceKey(mdLang.lang), false)
    private fun blockedGroups() = sourcePreferences.getString(getBlockedGroupsPrefKey(mdLang.lang), "").orEmpty()
    private fun blockedUploaders() = sourcePreferences.getString(getBlockedUploaderPrefKey(mdLang.lang), "").orEmpty()
    private fun coverQuality() = sourcePreferences.getString(getCoverQualityPrefKey(mdLang.lang), "").orEmpty()
    private fun tryUsingFirstVolumeCover() =
        sourcePreferences.getBoolean(getTryUsingFirstVolumeCoverKey(mdLang.lang), false)
    private fun altTitlesInDesc() = sourcePreferences.getBoolean(getAltTitlesInDescKey(mdLang.lang), false)
    private fun finalChapterInDesc() = sourcePreferences.getBoolean(getFinalChapterInDescPrefKey(mdLang.lang), false)
    private fun preferExtensionLangTitle() =
        sourcePreferences.getBoolean(preferExtensionLangTitleKey(mdLang.extLang), true)

    // UrlImportableSource methods
    override suspend fun mapUrlToMangaUrl(uri: Uri): String? {
        val lcFirstPathSegment = uri.pathSegments.firstOrNull()?.lowercase() ?: return null

        return if (lcFirstPathSegment == "title" || lcFirstPathSegment == "manga") {
            MdUtil.buildMangaUrl(uri.pathSegments[1])
        } else {
            null
        }
    }

    override fun mapUrlToChapterUrl(uri: Uri): String? {
        if (!uri.pathSegments.firstOrNull().equals("chapter", true)) return null
        val id = uri.pathSegments.getOrNull(1) ?: return null
        return MdApi.chapter + '/' + id
    }

    override suspend fun mapChapterUrlToMangaUrl(uri: Uri): String? {
        val id = uri.pathSegments.getOrNull(1) ?: return null
        return mangaHandler.getMangaFromChapterId(id)?.let { MdUtil.buildMangaUrl(it) }
    }

    @Deprecated("Use the non-RxJava API instead", replaceWith = ReplaceWith("getLatestUpdates"))
    override fun fetchLatestUpdates(page: Int): Observable<MangasPage> = runAsObservable { getLatestUpdates(page) }

    override suspend fun getLatestUpdates(page: Int): MangasPage {
        val request = delegateLatestUpdatesRequest(page)
        val url = request.url.newBuilder()
            .removeAllQueryParameters("includeFutureUpdates")
            .build()

        val response = client.newCall(request.newBuilder().url(url).build()).awaitSuccess()
        return delegateLatestUpdatesParse(response)
    }

    @Deprecated("Use the 1.x API instead", replaceWith = ReplaceWith("getMangaDetails"))
    override fun fetchMangaDetails(manga: SManga): Observable<SManga> = runAsObservable { getMangaDetails(manga) }

    /** The SY details for [manga] from the MangaDex API, with this source's cover and title preferences. */
    suspend fun getMangaDetails(manga: SManga): SManga = mangaHandler.getMangaDetails(
        manga,
        id,
        coverQuality(),
        tryUsingFirstVolumeCover(),
        altTitlesInDesc(),
        finalChapterInDesc(),
        preferExtensionLangTitle(),
    )

    @Deprecated("Use the 1.x API instead", replaceWith = ReplaceWith("getChapterList"))
    override fun fetchChapterList(manga: SManga): Observable<List<SChapter>> =
        mangaHandler.fetchChapterListObservable(manga, blockedGroups(), blockedUploaders())

    @Deprecated("Use the 1.x API instead", replaceWith = ReplaceWith("getPageList"))
    override fun fetchPageList(chapter: SChapter): Observable<List<Page>> =
        runAsObservable { pageHandler.fetchPageList(chapter, usePort443Only(), dataSaver(), delegate) }

    override suspend fun getPageList(chapter: SChapter): List<Page> =
        pageHandler.fetchPageList(chapter, usePort443Only(), dataSaver(), delegate)

    override suspend fun getImage(page: Page, existingSize: Long): Response {
        val call = pageHandler.getImageCall(page, existingSize)
        return call?.awaitSuccess() ?: super.getImage(page, existingSize)
    }

    @Deprecated("Use the non-RxJava API instead", replaceWith = ReplaceWith("getImageUrl"))
    override fun fetchImageUrl(page: Page): Observable<String> = runAsObservable { getImageUrl(page) }

    override suspend fun getImageUrl(page: Page): String {
        return pageHandler.getImageUrl(page) {
            super.getImageUrl(page)
        }
    }

    override fun newMetaInstance() = MangaDexSearchMetadata()

    override suspend fun parseIntoMetadata(
        metadata: MangaDexSearchMetadata,
        input: Triple<MangaDto, List<String>, StatisticsMangaDto>,
    ) {
        apiMangaParser.parseIntoMetadata(
            metadata,
            input.first,
            input.second,
            input.third,
            null,
            coverQuality(),
            altTitlesInDesc(),
            finalChapterInDesc(),
            preferExtensionLangTitle(),
        )
    }

    override fun isLogged(): Boolean = mdList.isLoggedIn

    override fun getUsername(): String = mdList.getUsername()

    override fun getPassword(): String = mdList.getPassword()

    override suspend fun login(authCode: String): Boolean = loginHelper.login(authCode)

    override suspend fun logout(): Boolean = loginHelper.logout()

    // FollowsSource methods
    override suspend fun fetchFollows(page: Int): MangasPage = followsHandler.fetchFollows(page)

    override suspend fun fetchAllFollows(): List<Pair<SManga, MangaDexSearchMetadata>> =
        followsHandler.fetchAllFollows()

    suspend fun updateFollowStatus(mangaID: String, followStatus: FollowStatus): Boolean =
        followsHandler.updateFollowStatus(mangaID, followStatus)

    suspend fun fetchTrackingInfo(url: String): Track = followsHandler.fetchTrackingInfo(url)

    // Tracker methods
    /*suspend fun updateReadingProgress(track: Track): Boolean {
        return followsHandler.updateReadingProgress(track)
    }*/

    suspend fun updateRating(track: Track): Boolean = followsHandler.updateRating(track)

    // RandomMangaSource method
    override suspend fun fetchRandomMangaUrl(): String = mangaHandler.fetchRandomMangaId()

    suspend fun getMangaSimilar(manga: SManga): MetadataMangasPage = similarHandler.getSimilar(manga)

    suspend fun getMangaRelated(manga: SManga): MetadataMangasPage = similarHandler.getRelated(manga)

    suspend fun getMangaMetadata(track: Track): SManga {
        return mangaHandler.getMangaMetadata(
            track,
            id,
            coverQuality(),
            tryUsingFirstVolumeCover(),
            altTitlesInDesc(),
            finalChapterInDesc(),
            preferExtensionLangTitle(),
        )
    }

    companion object {
        private const val dataSaverPref = "dataSaverV5"
        private const val standardHttpsPortPref = "usePort443"
        private const val blockedGroupsPref = "blockedGroups"
        private const val blockedUploaderPref = "blockedUploader"
        private const val coverQualityPref = "thumbnailQuality"
        private const val tryUsingFirstVolumeCoverPref = "tryUsingFirstVolumeCover"
        private const val altTitlesInDescPref = "altTitlesInDesc"
        private const val finalChapterInDescPref = "finalChapterInDesc"
        private const val preferExtensionLangTitlePref = "preferExtensionLangTitle"

        fun getDataSaverPreferenceKey(dexLang: String): String = "${dataSaverPref}_$dexLang"

        fun getStandardHttpsPreferenceKey(dexLang: String): String = "${standardHttpsPortPref}_$dexLang"

        fun getBlockedGroupsPrefKey(dexLang: String): String = "${blockedGroupsPref}_$dexLang"

        fun getBlockedUploaderPrefKey(dexLang: String): String = "${blockedUploaderPref}_$dexLang"

        fun getCoverQualityPrefKey(dexLang: String): String = "${coverQualityPref}_$dexLang"

        fun getTryUsingFirstVolumeCoverKey(dexLang: String): String = "${tryUsingFirstVolumeCoverPref}_$dexLang"

        fun getAltTitlesInDescKey(dexLang: String): String = "${altTitlesInDescPref}_$dexLang"

        fun getFinalChapterInDescPrefKey(dexLang: String): String = "${finalChapterInDescPref}_$dexLang"

        fun preferExtensionLangTitleKey(dexLang: String): String = "${preferExtensionLangTitlePref}_$dexLang"
    }
}
