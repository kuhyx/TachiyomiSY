package exh.source

import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SMangaUpdate
import eu.kanade.tachiyomi.source.online.HttpSource
import okhttp3.Request
import rx.Observable

/** SY: the manga details, chapter list and URL helpers of an [EnhancedHttpSource]. */
@Suppress("OverridingDeprecatedMember", "DEPRECATION")
public abstract class EnhancedHttpSourceManga(
    originalSource: HttpSource,
    enhancedSource: HttpSource,
) : EnhancedHttpSourceBrowse(originalSource, enhancedSource) {
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

    override fun getMangaUrl(manga: SManga): String = source().getMangaUrl(manga)

    override fun getChapterUrl(chapter: SChapter): String = source().getChapterUrl(chapter)

    @Deprecated("All modifications should be done when constructing the chapter")
    override fun prepareNewChapter(chapter: SChapter, manga: SManga) {
        source().prepareNewChapter(chapter, manga)
    }

    override fun getFilterList(): FilterList = source().getFilterList()
}
