package eu.kanade.tachiyomi.ui.reader

import eu.kanade.domain.chapter.model.toDbChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.util.chapter.filterDownloaded
import eu.kanade.tachiyomi.util.chapter.removeDuplicates
import exh.source.MERGED_SOURCE_ID
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.service.getChapterSort
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.bookmarkedFilterRaw
import tachiyomi.domain.manga.model.downloadedFilterRaw
import tachiyomi.domain.manga.model.unreadFilterRaw
import uy.kohesive.injekt.api.get

internal fun ReaderViewModel.buildChapterList(): List<ReaderChapter> {
    val manga = manga!!
    // SY -->
    val (chapters, mangaMap) = runBlocking {
        if (manga.source == MERGED_SOURCE_ID) {
            getMergedChaptersByMangaId.await(manga.id, applyScanlatorFilter = true) to
                getMergedMangaById.await(manga.id)
                    .associateBy { it.id }
        } else {
            getChaptersByMangaId.await(manga.id, applyScanlatorFilter = true) to null
        }
    }
    // SY <--

    val selectedChapter = chapters.find { it.id == chapterId }
        ?: error("Requested chapter of id $chapterId not found in chapter list")

    return applySkipPreferences(chapters, selectedChapter, manga, mangaMap)
        .sortedWith(getChapterSort(manga, sortDescending = false))
        .let { if (readerPreferences.skipDupe.get()) it.removeDuplicates(selectedChapter) else it }
        .let { if (basePreferences.downloadedOnly.get()) it.filterDownloaded(manga, mangaMap) else it }
        .map { it.toDbChapter() }
        .map(::ReaderChapter)
}

// The chapters left after the skip-read / skip-filtered preferences; the selected chapter always stays.
private fun ReaderViewModel.applySkipPreferences(
    chapters: List<Chapter>,
    selectedChapter: Chapter,
    manga: Manga,
    mangaMap: Map<Long, Manga>?,
): List<Chapter> {
    if (!readerPreferences.skipRead.get() && !readerPreferences.skipFiltered.get()) return chapters
    val filteredChapters = chapters.filterNot { isSkipped(it, manga, mangaMap) }
    return if (filteredChapters.any { it.id == chapterId }) {
        filteredChapters
    } else {
        filteredChapters + listOf(selectedChapter)
    }
}

// Whether the reader's skip-read / skip-filtered preferences hide [chapter] from the reader.
private fun ReaderViewModel.isSkipped(chapter: Chapter, manga: Manga, mangaMap: Map<Long, Manga>?): Boolean = when {
    readerPreferences.skipRead.get() && chapter.read -> true
    readerPreferences.skipFiltered.get() ->
        hiddenByReadFilter(chapter, manga) ||
            // SY -->
            hiddenByDownloadFilter(chapter, manga, mangaMap) ||
            // SY <--
            hiddenByBookmarkFilter(chapter, manga)
    else -> false
}

private fun hiddenByReadFilter(chapter: Chapter, manga: Manga): Boolean = when (manga.unreadFilterRaw) {
    Manga.CHAPTER_SHOW_READ -> !chapter.read
    Manga.CHAPTER_SHOW_UNREAD -> chapter.read
    else -> false
}

private fun hiddenByBookmarkFilter(chapter: Chapter, manga: Manga): Boolean = when (manga.bookmarkedFilterRaw) {
    Manga.CHAPTER_SHOW_BOOKMARKED -> !chapter.bookmark
    Manga.CHAPTER_SHOW_NOT_BOOKMARKED -> chapter.bookmark
    else -> false
}

// SY -->
private fun ReaderViewModel.hiddenByDownloadFilter(
    chapter: Chapter,
    manga: Manga,
    mangaMap: Map<Long, Manga>?,
): Boolean = when (manga.downloadedFilterRaw) {
    Manga.CHAPTER_SHOW_DOWNLOADED -> !isChapterDownloaded(chapter, manga, mangaMap)
    Manga.CHAPTER_SHOW_NOT_DOWNLOADED -> isChapterDownloaded(chapter, manga, mangaMap)
    else -> false
}

private fun ReaderViewModel.isChapterDownloaded(chapter: Chapter, manga: Manga, mangaMap: Map<Long, Manga>?): Boolean {
    val chapterManga = mangaMap?.get(chapter.mangaId) ?: manga
    return downloadManager.isChapterDownloaded(
        chapterName = chapter.name,
        chapterScanlator = chapter.scanlator,
        chapterUrl = chapter.url,
        mangaTitle = chapterManga.ogTitle,
        sourceId = chapterManga.source,
    )
}
// SY <--
