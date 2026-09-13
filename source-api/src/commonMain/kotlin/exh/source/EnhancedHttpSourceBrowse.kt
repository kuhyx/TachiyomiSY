package exh.source

import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.online.HttpSource
import exh.pref.DelegateSourcePreferences
import okhttp3.Headers
import okhttp3.OkHttpClient
import rx.Observable
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** SY: the identity and browse listings of an [EnhancedHttpSource], all read from the active [source].
 *
 * @property originalSource the extension source.
 * @property enhancedSource the in-app replacement. */
@Suppress("OverridingDeprecatedMember", "DEPRECATION")
public abstract class EnhancedHttpSourceBrowse(
    public val originalSource: HttpSource,
    public val enhancedSource: HttpSource,
) : UnsupportedHelpersHttpSource() {
    override val baseUrl: String get() = source().baseUrl

    override val headers: Headers get() = source().headers

    override val supportsLatest: Boolean get() = source().supportsLatest

    override val name: String get() = source().name

    override val lang: String get() = source().lang

    override val id: Long get() = source().id

    override val client: OkHttpClient get() = originalSource.client // source().client

    /** The active source: enhanced when delegated sources are enabled, original otherwise. */
    public fun source(): HttpSource {
        return if (Injekt.get<DelegateSourcePreferences>().delegateSources.get()) {
            enhancedSource
        } else {
            originalSource
        }
    }

    override fun getHomeUrl(): String = source().getHomeUrl()

    // ===> OPTIONAL FIELDS

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
}
