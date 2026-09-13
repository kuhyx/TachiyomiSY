package exh.source

import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.online.HttpSource
import okhttp3.Response
import rx.Observable

/** SY: an in-app replacement for an extension source; every request goes through the [delegate].
 *
 * @param delegate the extension source being wrapped. */
@Suppress("OverridingDeprecatedMember", "DEPRECATION")
public abstract class DelegatedHttpSource(delegate: HttpSource) : DelegatedHttpSourceManga(delegate) {
    init {
        delegate.bindDelegate(this)
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

    /** Thrown when the delegate's version or language differs from this source's. */
    public class IncompatibleDelegateException(message: String) : RuntimeException(message)
}
