package exh.source

import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SMangaUpdate
import eu.kanade.tachiyomi.source.online.HttpSource
import okhttp3.Request
import rx.Observable

/** SY: the manga details, chapter list and URL helpers of a [DelegatedHttpSource]. */
@Suppress("OverridingDeprecatedMember", "DEPRECATION")
public abstract class DelegatedHttpSourceManga(delegate: HttpSource) : DelegatedHttpSourceBrowse(delegate) {
    @Deprecated("Use the 1.x API instead", replaceWith = ReplaceWith("getMangaDetails"))
    override fun fetchMangaDetails(manga: SManga): Observable<SManga> {
        ensureDelegateCompatible()
        return delegate.fetchMangaDetails(manga)
    }

    override suspend fun getMangaUpdate(
        manga: SManga,
        chapters: List<SChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ): SMangaUpdate {
        ensureDelegateCompatible()
        return delegate.getMangaUpdate(manga, chapters, fetchDetails, fetchChapters)
    }

    @Deprecated(HELPER_DEPRECATION)
    override fun mangaDetailsRequest(manga: SManga): Request {
        ensureDelegateCompatible()
        return delegate.mangaDetailsRequest(manga)
    }

    @Deprecated("Use the 1.x API instead", replaceWith = ReplaceWith("getChapterList"))
    override fun fetchChapterList(manga: SManga): Observable<List<SChapter>> {
        ensureDelegateCompatible()
        return delegate.fetchChapterList(manga)
    }

    override fun getMangaUrl(manga: SManga): String {
        ensureDelegateCompatible()
        return delegate.getMangaUrl(manga)
    }

    override fun getChapterUrl(chapter: SChapter): String {
        ensureDelegateCompatible()
        return delegate.getChapterUrl(chapter)
    }

    @Deprecated("All modifications should be done when constructing the chapter")
    override fun prepareNewChapter(chapter: SChapter, manga: SManga) {
        ensureDelegateCompatible()
        return delegate.prepareNewChapter(chapter, manga)
    }

    override fun getFilterList(): FilterList = delegate.getFilterList()
}
