package exh.source

import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SMangaUpdate
import eu.kanade.tachiyomi.source.online.HttpSource
import exh.pref.DelegateSourcePreferences
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import rx.Observable
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Suppress("OverridingDeprecatedMember", "DEPRECATION")
public class EnhancedHttpSource(
    public val originalSource: HttpSource,
    public val enhancedSource: HttpSource,
) : HttpSource() {

    @Deprecated(HELPER_DEPRECATION)
    override fun popularMangaRequest(page: Int): Request =
        throw UnsupportedOperationException("Should never be called!")

    @Deprecated(HELPER_DEPRECATION)
    override fun popularMangaParse(response: Response): MangasPage =
        throw UnsupportedOperationException("Should never be called!")

    @Deprecated(HELPER_DEPRECATION)
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request =
        throw UnsupportedOperationException("Should never be called!")

    @Deprecated(HELPER_DEPRECATION)
    override fun searchMangaParse(response: Response): MangasPage =
        throw UnsupportedOperationException("Should never be called!")

    @Deprecated(HELPER_DEPRECATION)
    override fun latestUpdatesRequest(page: Int): Request =
        throw UnsupportedOperationException("Should never be called!")

    @Deprecated(HELPER_DEPRECATION)
    override fun latestUpdatesParse(response: Response): MangasPage =
        throw UnsupportedOperationException("Should never be called!")

    @Deprecated(HELPER_DEPRECATION)
    override fun mangaDetailsParse(response: Response): SManga =
        throw UnsupportedOperationException("Should never be called!")

    @Deprecated(HELPER_DEPRECATION)
    override fun chapterListParse(response: Response): List<SChapter> =
        throw UnsupportedOperationException("Should never be called!")

    @Deprecated(HELPER_DEPRECATION)
    override fun pageListParse(response: Response): List<Page> =
        throw UnsupportedOperationException("Should never be called!")

    @Deprecated(HELPER_DEPRECATION)
    override fun imageUrlParse(response: Response): String =
        throw UnsupportedOperationException("Should never be called!")

    override val baseUrl: String get() = source().baseUrl

    override fun getHomeUrl(): String = source().getHomeUrl()

    override val headers: Headers get() = source().headers

    override val supportsLatest: Boolean get() = source().supportsLatest

    override val name: String get() = source().name

    override val lang: String get() = source().lang

    // ===> OPTIONAL FIELDS

    override val id: Long get() = source().id

    override val client: OkHttpClient get() = originalSource.client // source().client

    override fun toString(): String = source().toString()

    @Deprecated("Use the non-RxJava API instead", replaceWith = ReplaceWith("getPopularManga"))
    override fun fetchPopularManga(page: Int): Observable<MangasPage> = source().fetchPopularManga(page)

    override suspend fun getPopularManga(page: Int): MangasPage = source().getPopularManga(page)

    @Deprecated("Use the non-RxJava API instead", replaceWith = ReplaceWith("getSearchManga"))
    override fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> =
        source().fetchSearchManga(page, query, filters)

    override suspend fun getSearchManga(page: Int, query: String, filters: FilterList): MangasPage =
        source().getSearchManga(page, query, filters)

    @Deprecated("Use the non-RxJava API instead", replaceWith = ReplaceWith("getLatestUpdates"))
    override fun fetchLatestUpdates(page: Int): Observable<MangasPage> = source().fetchLatestUpdates(page)

    override suspend fun getLatestUpdates(page: Int): MangasPage = source().getLatestUpdates(page)

    @Deprecated("Use the 1.x API instead", replaceWith = ReplaceWith("getMangaDetails"))
    override fun fetchMangaDetails(manga: SManga): Observable<SManga> = source().fetchMangaDetails(manga)

    override suspend fun getMangaUpdate(
        manga: SManga,
        chapters: List<SChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ): SMangaUpdate = source().getMangaUpdate(manga, chapters, fetchDetails, fetchChapters)

    @Deprecated(HELPER_DEPRECATION)
    override fun mangaDetailsRequest(manga: SManga): Request = source().mangaDetailsRequest(manga)

    @Deprecated("Use the 1.x API instead", replaceWith = ReplaceWith("getChapterList"))
    override fun fetchChapterList(manga: SManga): Observable<List<SChapter>> = source().fetchChapterList(manga)

    @Deprecated("Use the 1.x API instead", replaceWith = ReplaceWith("getPageList"))
    override fun fetchPageList(chapter: SChapter): Observable<List<Page>> = source().fetchPageList(chapter)

    override suspend fun getPageList(chapter: SChapter): List<Page> = source().getPageList(chapter)

    @Deprecated("Use the non-RxJava API instead", replaceWith = ReplaceWith("getImageUrl"))
    override fun fetchImageUrl(page: Page): Observable<String> = source().fetchImageUrl(page)

    override suspend fun getImageUrl(page: Page): String = source().getImageUrl(page)

    override suspend fun getImage(page: Page, existingSize: Long): Response = source().getImage(page, existingSize)

    override fun getMangaUrl(manga: SManga): String = source().getMangaUrl(manga)

    override fun getChapterUrl(chapter: SChapter): String = source().getChapterUrl(chapter)

    @Deprecated("All modifications should be done when constructing the chapter")
    override fun prepareNewChapter(chapter: SChapter, manga: SManga) {
        source().prepareNewChapter(chapter, manga)
    }

    override fun getFilterList(): FilterList = source().getFilterList()

    public fun source(): HttpSource {
        return if (Injekt.get<DelegateSourcePreferences>().delegateSources.get()) {
            enhancedSource
        } else {
            originalSource
        }
    }
}
