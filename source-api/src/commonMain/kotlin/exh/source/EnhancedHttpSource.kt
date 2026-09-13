package exh.source

import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.online.HttpSource
import okhttp3.Response
import rx.Observable

/** SY: a source that switches between an extension and its in-app enhanced twin by preference.
 *
 * @param originalSource the extension source.
 * @param enhancedSource the in-app replacement. */
@Suppress("OverridingDeprecatedMember", "DEPRECATION")
public class EnhancedHttpSource(
    originalSource: HttpSource,
    enhancedSource: HttpSource,
) : EnhancedHttpSourceManga(originalSource, enhancedSource) {
    @Deprecated("Use the 1.x API instead", replaceWith = ReplaceWith("getPageList"))
    override fun fetchPageList(chapter: SChapter): Observable<List<Page>> = source().fetchPageList(chapter)

    override suspend fun getPageList(chapter: SChapter): List<Page> = source().getPageList(chapter)

    @Deprecated("Use the non-RxJava API instead", replaceWith = ReplaceWith("getImageUrl"))
    override fun fetchImageUrl(page: Page): Observable<String> = source().fetchImageUrl(page)

    override suspend fun getImageUrl(page: Page): String = source().getImageUrl(page)

    override suspend fun getImage(page: Page, existingSize: Long): Response = source().getImage(page, existingSize)
}
