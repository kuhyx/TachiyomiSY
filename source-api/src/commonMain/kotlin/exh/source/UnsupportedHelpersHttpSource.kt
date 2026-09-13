package exh.source

import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import okhttp3.Request
import okhttp3.Response

/**
 * SY: an [HttpSource] whose request/parse helper pairs are never called because every
 * fetch is forwarded to another source; each helper throws if it is reached anyway.
 */
@Suppress("OverridingDeprecatedMember", "DEPRECATION")
public abstract class UnsupportedHelpersHttpSource : HttpSource() {
    @Deprecated(HELPER_DEPRECATION)
    override fun popularMangaRequest(page: Int): Request =
        throw UnsupportedOperationException(NEVER_CALLED)

    @Deprecated(HELPER_DEPRECATION)
    override fun popularMangaParse(response: Response): MangasPage =
        throw UnsupportedOperationException(NEVER_CALLED)

    @Deprecated(HELPER_DEPRECATION)
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request =
        throw UnsupportedOperationException(NEVER_CALLED)

    @Deprecated(HELPER_DEPRECATION)
    override fun searchMangaParse(response: Response): MangasPage =
        throw UnsupportedOperationException(NEVER_CALLED)

    @Deprecated(HELPER_DEPRECATION)
    override fun latestUpdatesRequest(page: Int): Request =
        throw UnsupportedOperationException(NEVER_CALLED)

    @Deprecated(HELPER_DEPRECATION)
    override fun latestUpdatesParse(response: Response): MangasPage =
        throw UnsupportedOperationException(NEVER_CALLED)

    @Deprecated(HELPER_DEPRECATION)
    override fun mangaDetailsParse(response: Response): SManga =
        throw UnsupportedOperationException(NEVER_CALLED)

    @Deprecated(HELPER_DEPRECATION)
    override fun chapterListParse(response: Response): List<SChapter> =
        throw UnsupportedOperationException(NEVER_CALLED)

    @Deprecated(HELPER_DEPRECATION)
    override fun pageListParse(response: Response): List<Page> =
        throw UnsupportedOperationException(NEVER_CALLED)

    @Deprecated(HELPER_DEPRECATION)
    override fun imageUrlParse(response: Response): String =
        throw UnsupportedOperationException(NEVER_CALLED)
}
