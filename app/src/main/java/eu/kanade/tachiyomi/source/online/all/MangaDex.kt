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
import exh.md.handlers.MangaDetailsExtras
import exh.md.network.MangaDexLoginHelper
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

    internal val mdLang by lazy {
        MdLang.fromExt(lang) ?: MdLang.ENGLISH
    }

    override val matchingHosts: List<String> = listOf("mangadex.org", "www.mangadex.org")

    val trackPreferences: TrackPreferences by injectLazy()
    val mdList: MdList by lazy { Injekt.get<TrackerManager>().mdList }

    internal val sourcePreferences: SharedPreferences by lazy {
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

    private val handlers by lazy { MangaDexHandlers(this, network) }

    // MetadataSource methods
    override val metaClass: KClass<MangaDexSearchMetadata> = MangaDexSearchMetadata::class

    // LoginSource methods
    override val requiresLogin: Boolean = false

    override val twoFactorAuth = LoginSource.AuthSupport.NOT_SUPPORTED

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
        return handlers.mangaHandler.getMangaFromChapterId(id)?.let { MdUtil.buildMangaUrl(it) }
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
    suspend fun getMangaDetails(manga: SManga): SManga =
        handlers.mangaHandler.getMangaDetails(manga, id, detailsPreferences())

    @Deprecated("Use the 1.x API instead", replaceWith = ReplaceWith("getChapterList"))
    override fun fetchChapterList(manga: SManga): Observable<List<SChapter>> =
        handlers.mangaHandler.fetchChapterListObservable(manga, blockedGroups(), blockedUploaders())

    @Deprecated("Use the 1.x API instead", replaceWith = ReplaceWith("getPageList"))
    override fun fetchPageList(chapter: SChapter): Observable<List<Page>> =
        runAsObservable { handlers.pageHandler.fetchPageList(chapter, usePort443Only(), dataSaver(), delegate) }

    override suspend fun getPageList(chapter: SChapter): List<Page> =
        handlers.pageHandler.fetchPageList(chapter, usePort443Only(), dataSaver(), delegate)

    override suspend fun getImage(page: Page, existingSize: Long): Response {
        val call = handlers.pageHandler.getImageCall(page, existingSize)
        return call?.awaitSuccess() ?: super.getImage(page, existingSize)
    }

    @Deprecated("Use the non-RxJava API instead", replaceWith = ReplaceWith("getImageUrl"))
    override fun fetchImageUrl(page: Page): Observable<String> = runAsObservable { getImageUrl(page) }

    override suspend fun getImageUrl(page: Page): String {
        return handlers.pageHandler.getImageUrl(page) {
            super.getImageUrl(page)
        }
    }

    override fun newMetaInstance() = MangaDexSearchMetadata()

    override suspend fun parseIntoMetadata(
        metadata: MangaDexSearchMetadata,
        input: Triple<MangaDto, List<String>, StatisticsMangaDto>,
    ) {
        handlers.apiMangaParser.parseIntoMetadata(
            metadata,
            input.first,
            MangaDetailsExtras(input.second, input.third, coverFileName = null),
            detailsPreferences(),
        )
    }

    override fun isLogged(): Boolean = mdList.isLoggedIn

    override fun getUsername(): String = mdList.getUsername()

    override fun getPassword(): String = mdList.getPassword()

    override suspend fun login(authCode: String): Boolean = loginHelper.login(authCode)

    override suspend fun logout(): Boolean = loginHelper.logout()

    // FollowsSource methods
    override suspend fun fetchFollows(page: Int): MangasPage = handlers.followsHandler.fetchFollows(page)

    override suspend fun fetchAllFollows(): List<Pair<SManga, MangaDexSearchMetadata>> =
        handlers.followsHandler.fetchAllFollows()

    suspend fun updateFollowStatus(mangaID: String, followStatus: FollowStatus): Boolean =
        handlers.followsHandler.updateFollowStatus(mangaID, followStatus)

    suspend fun fetchTrackingInfo(url: String): Track = handlers.followsHandler.fetchTrackingInfo(url)

    // Tracker methods
    /*suspend fun updateReadingProgress(track: Track): Boolean {
        return handlers.followsHandler.updateReadingProgress(track)
    }*/

    suspend fun updateRating(track: Track): Boolean = handlers.followsHandler.updateRating(track)

    // RandomMangaSource method
    override suspend fun fetchRandomMangaUrl(): String = handlers.mangaHandler.fetchRandomMangaId()

    suspend fun getMangaSimilar(manga: SManga): MetadataMangasPage = handlers.similarHandler.getSimilar(manga)

    suspend fun getMangaRelated(manga: SManga): MetadataMangasPage = handlers.similarHandler.getRelated(manga)

    suspend fun getMangaMetadata(track: Track): SManga =
        handlers.mangaHandler.getMangaMetadata(track, id, detailsPreferences())

    companion object {
        internal const val dataSaverPref = "dataSaverV5"
        internal const val standardHttpsPortPref = "usePort443"
        internal const val blockedGroupsPref = "blockedGroups"
        internal const val blockedUploaderPref = "blockedUploader"
        internal const val coverQualityPref = "thumbnailQuality"
        internal const val tryUsingFirstVolumeCoverPref = "tryUsingFirstVolumeCover"
        internal const val altTitlesInDescPref = "altTitlesInDesc"
        internal const val finalChapterInDescPref = "finalChapterInDesc"
        internal const val preferExtensionLangTitlePref = "preferExtensionLangTitle"
    }
}
