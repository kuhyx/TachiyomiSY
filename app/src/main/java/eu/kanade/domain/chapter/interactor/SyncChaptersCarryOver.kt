package eu.kanade.domain.chapter.interactor

import eu.kanade.domain.chapter.interactor.SyncChaptersWithSource.Diff
import exh.source.isEhBasedManga
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.model.toChapterUpdate
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.model.Manga
import java.lang.Long.max
import java.time.ZonedDateTime
import java.util.TreeSet

// Stamps the new chapters with a descending fetch date and carries read/bookmark state over from
// the removed chapters that share a chapter number (a re-uploaded chapter keeps its progress).
internal fun SyncChaptersWithSource.carryOverRemovedState(
    diff: Diff,
    dbChapters: List<Chapter>,
    nowMillis: Long,
    changedOrDuplicateReadUrls: MutableSet<String>,
): List<Chapter> {
    val deletedChapterNumbers = TreeSet<Double>()
    val deletedReadChapterNumbers = TreeSet<Double>()
    val deletedBookmarkedChapterNumbers = TreeSet<Double>()
    val readChapterNumbers = dbChapters
        .asSequence()
        .filter { it.read && it.isRecognizedNumber }
        .map { it.chapterNumber }
        .toSet()
    diff.removed.forEach { chapter ->
        if (chapter.read) deletedReadChapterNumbers.add(chapter.chapterNumber)
        if (chapter.bookmark) deletedBookmarkedChapterNumbers.add(chapter.chapterNumber)
        deletedChapterNumbers.add(chapter.chapterNumber)
    }
    val deletedChapterNumberDateFetchMap = diff.removed.sortedByDescending { it.dateFetch }
        .associate { it.chapterNumber to it.dateFetch }
    val markDuplicateAsRead = libraryPreferences.markDuplicateReadChapterAsRead.get()
        .contains(LibraryPreferences.MARK_DUPLICATE_CHAPTER_READ_NEW)

    // Date fetch is set in such a way that the upper ones will have bigger value than the lower ones
    // Sources MUST return the chapters from most to less recent, which is common.
    var itemCount = diff.new.size
    return diff.new.map { toAddItem ->
        var chapter = toAddItem.copy(dateFetch = nowMillis + itemCount--)
        if (chapter.chapterNumber in readChapterNumbers && markDuplicateAsRead) {
            changedOrDuplicateReadUrls.add(chapter.url)
            chapter = chapter.copy(read = true)
        }
        if (!chapter.isRecognizedNumber || chapter.chapterNumber !in deletedChapterNumbers) {
            chapter
        } else {
            chapter = chapter.copy(
                read = chapter.chapterNumber in deletedReadChapterNumbers,
                bookmark = chapter.chapterNumber in deletedBookmarkedChapterNumbers,
            )
            // Try to to use the fetch date of the original entry to not pollute 'Updates' tab
            deletedChapterNumberDateFetchMap[chapter.chapterNumber]?.let {
                chapter = chapter.copy(dateFetch = it)
            }
            changedOrDuplicateReadUrls.add(chapter.url)
            chapter
        }
    }
}

// EXH: an E-Hentai gallery is one "chapter" per revision, so a new revision inherits the page reached.
internal fun SyncChaptersWithSource.carryOverEhProgress(
    manga: Manga,
    dbChapters: List<Chapter>,
    toAdd: List<Chapter>,
    changedOrDuplicateReadUrls: Set<String>,
): List<Chapter> {
    val max = dbChapters.maxOfOrNull { it.lastPageRead } ?: 0
    val applies = manga.isEhBasedManga() && max > 0 && toAdd.any { it.url !in changedOrDuplicateReadUrls }
    if (!applies) return toAdd
    return toAdd.map { if (it.url !in changedOrDuplicateReadUrls) it.copy(lastPageRead = max) else it }
}

// Writes the diff and returns the inserted chapters with their db ids.
internal suspend fun SyncChaptersWithSource.persist(
    diff: Diff,
    toAdd: List<Chapter>,
    manga: Manga,
    now: ZonedDateTime,
    fetchWindow: Pair<Long, Long>,
): List<Chapter> {
    if (diff.removed.isNotEmpty()) {
        chapterRepository.removeChaptersWithIds(diff.removed.map { it.id })
    }
    val added = if (toAdd.isNotEmpty()) chapterRepository.addAll(toAdd) else toAdd
    if (diff.updated.isNotEmpty()) {
        updateChapter.awaitAll(diff.updated.map { it.toChapterUpdate() })
    }
    updateManga.awaitUpdateFetchInterval(manga, now, fetchWindow)
    // Set this manga as updated since chapters were changed
    // Note that last_update actually represents last time the chapter list changed at all
    updateManga.awaitUpdateLastUpdate(manga.id)
    return added
}
