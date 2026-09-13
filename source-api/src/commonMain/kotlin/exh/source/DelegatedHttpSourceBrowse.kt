package exh.source

import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.online.HttpSource
import okhttp3.Headers
import okhttp3.OkHttpClient
import rx.Observable

/** SY: the identity and browse listings of a [DelegatedHttpSource], all read from the [delegate].
 *
 * @property delegate the extension source being wrapped. */
@Suppress("OverridingDeprecatedMember", "DEPRECATION")
public abstract class DelegatedHttpSourceBrowse(public val delegate: HttpSource) : UnsupportedHelpersHttpSource() {
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

    /** Throws [DelegatedHttpSource.IncompatibleDelegateException] unless the delegate's version and language match. */
    protected open fun ensureDelegateCompatible() {
        if (versionId != delegate.versionId || lang != delegate.lang) {
            throw DelegatedHttpSource.IncompatibleDelegateException(
                "Delegate source is not compatible (" +
                    "versionId: $versionId <=> ${delegate.versionId}, lang: $lang <=> ${delegate.lang}" +
                    ")!",
            )
        }
    }
}
