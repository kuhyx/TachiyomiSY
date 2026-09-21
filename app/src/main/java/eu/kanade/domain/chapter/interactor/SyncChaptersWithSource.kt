package eu.kanade.domain.chapter.interactor

import eu.kanade.domain.chapter.model.copyFromSChapter
import eu.kanade.domain.chapter.model.toSChapter
import eu.kanade.domain.manga.interactor.GetExcludedScanlators
import eu.kanade.domain.manga.interactor.UpdateManga
import eu.kanade.domain.manga.model.toSManga
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.download.DownloadProvider
import eu.kanade.tachiyomi.data.download.isChapterDirNameChanged
import eu.kanade.tachiyomi.data.download.renameChapter
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.online.HttpSource
import exh.source.isEhBasedManga
import tachiyomi.data.chapter.ChapterSanitizer
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.chapter.interactor.ShouldUpdateDbChapter
import tachiyomi.domain.chapter.interactor.UpdateChapter
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.model.NoChaptersException
import tachiyomi.domain.chapter.model.toChapterUpdate
import tachiyomi.domain.chapter.repository.ChapterRepository
import tachiyomi.domain.chapter.service.ChapterRecognition
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.model.Manga
import tachiyomi.source.local.isLocal
import java.lang.Long.max
import java.time.ZonedDateTime
import java.util.TreeSet

internal class SyncChaptersWithSource(collaborators: Collaborators) {
    private val downloadManager = collaborators.downloadManager
    private val downloadProvider = collaborators.downloadProvider
    private val chapterRepository = collaborators.chapterRepository
    private val shouldUpdateDbChapter = collaborators.shouldUpdateDbChapter
    private val updateManga = collaborators.updateManga
    private val updateChapter = collaborators.updateChapter
    private val getChaptersByMangaId = collaborators.getChaptersByMangaId
    private val getExcludedScanlators = collaborators.getExcludedScanlators
    private val libraryPreferences = collaborators.libraryPreferences

    /** Everything [SyncChaptersWithSource] talks to, resolved by the DI graph. */
    data class Collaborators(
        val downloadManager: DownloadManager,
        val downloadProvider: DownloadProvider,
        val chapterRepository: ChapterRepository,
        val shouldUpdateDbChapter: ShouldUpdateDbChapter,
        val updateManga: UpdateManga,
        val updateChapter: UpdateChapter,
        val getChaptersByMangaId: GetChaptersByMangaId,
        val getExcludedScanlators: GetExcludedScanlators,
        val libraryPreferences: LibraryPreferences,
    )

    /**
     * Method to synchronize db chapters with source ones.
     *
     * @param rawSourceChapters the chapters from the source.
     * @param manga the manga the chapters belong to.
     * @param source the source the manga belongs to.
     * @param manualFetch whether the user asked for this refresh.
     * @param fetchWindow the current update window used to keep the next-update estimate.
     * @return Newly added chapters
     */
    suspend fun await(
        rawSourceChapters: List<SChapter>,
        manga: Manga,
        source: Source,
        manualFetch: Boolean = false,
        fetchWindow: Pair<Long, Long> = Pair(0, 0),
    ): List<Chapter> {
        if (rawSourceChapters.isEmpty() && !source.isLocal()) {
            throw NoChaptersException()
        }

        val now = ZonedDateTime.now()
        val nowMillis = now.toInstant().toEpochMilli()
        val sourceChapters = toChapters(rawSourceChapters, manga)
        val dbChapters = getChaptersByMangaId.await(manga.id)
        val diff = diff(sourceChapters, dbChapters, manga, source, nowMillis)

        // Return if there's nothing to add, delete, or update to avoid unnecessary db transactions.
        if (diff.isEmpty()) {
            if (manualFetch || manga.fetchInterval == 0 || manga.nextUpdate < fetchWindow.first) {
                updateManga.awaitUpdateFetchInterval(manga, now, fetchWindow)
            }
            return emptyList()
        }

        val changedOrDuplicateReadUrls = mutableSetOf<String>()
        var toAdd = carryOverRemovedState(diff, dbChapters, nowMillis, changedOrDuplicateReadUrls)
        toAdd = carryOverEhProgress(manga, dbChapters, toAdd, changedOrDuplicateReadUrls)
        toAdd = persist(diff, toAdd, manga, now, fetchWindow)

        val excludedScanlators = getExcludedScanlators.await(manga.id).toHashSet()
        return toAdd.filterNot { it.url in changedOrDuplicateReadUrls || it.scanlator in excludedScanlators }
    }

    private fun toChapters(rawSourceChapters: List<SChapter>, manga: Manga): List<Chapter> {
        return rawSourceChapters
            .distinctBy { it.url }
            .mapIndexed { i, sChapter ->
                Chapter.create()
                    .copyFromSChapter(sChapter)
                    .copy(name = with(ChapterSanitizer) { sChapter.name.sanitize(manga.title) })
                    .copy(mangaId = manga.id, sourceOrder = i.toLong())
            }
    }

    /** The source list against the db: what to insert, what to update in place, what disappeared. */
    private class Diff(
        val new: MutableList<Chapter> = mutableListOf(),
        val updated: MutableList<Chapter> = mutableListOf(),
        val removed: List<Chapter>,
    ) {
        fun isEmpty(): Boolean = new.isEmpty() && removed.isEmpty() && updated.isEmpty()
    }

    private suspend fun diff(
        sourceChapters: List<Chapter>,
        dbChapters: List<Chapter>,
        manga: Manga,
        source: Source,
        nowMillis: Long,
    ): Diff {
        val diff = Diff(
            removed = dbChapters.filterNot { dbChapter -> sourceChapters.any { it.url == dbChapter.url } },
        )
        // Used to not set upload date of older chapters
        // to a higher value than newer chapters
        var maxSeenUploadDate = 0L

        for (sourceChapter in sourceChapters) {
            val chapter = prepare(sourceChapter, manga, source)
            val dbChapter = dbChapters.find { it.url == chapter.url }
            when {
                dbChapter == null -> {
                    // A missing upload date borrows the newest one seen so far, or "now" for the first.
                    val fallbackDate = if (maxSeenUploadDate == 0L) nowMillis else maxSeenUploadDate
                    diff.new.add(chapter.withUploadDate(fallbackDate))
                    maxSeenUploadDate = max(maxSeenUploadDate, sourceChapter.dateUpload)
                }
                shouldUpdateDbChapter.await(dbChapter, chapter) -> {
                    diff.updated.add(updatedDbChapter(dbChapter, chapter, manga, source))
                }
            }
        }
        return diff
    }

    private fun Chapter.withUploadDate(fallback: Long): Chapter =
        if (dateUpload == 0L) copy(dateUpload = fallback) else this

    // Applies the source's per-chapter hook and recognises the chapter number.
    private fun prepare(sourceChapter: Chapter, manga: Manga, source: Source): Chapter {
        var chapter = sourceChapter
        // Update metadata from source if necessary.
        if (source is HttpSource) {
            val sChapter = chapter.toSChapter()
            @Suppress("DEPRECATION")
            source.prepareNewChapter(sChapter, manga.toSManga())
            chapter = chapter.copyFromSChapter(sChapter)
        }
        val chapterNumber = ChapterRecognition.parseChapterNumber(manga.title, chapter.name, chapter.chapterNumber)
        return chapter.copy(chapterNumber = chapterNumber)
    }

    // The db row refreshed from the source, renaming the download directory when the name moved.
    private suspend fun updatedDbChapter(dbChapter: Chapter, chapter: Chapter, manga: Manga, source: Source): Chapter {
        val shouldRenameChapter = downloadProvider.isChapterDirNameChanged(dbChapter, chapter) &&
            downloadManager.isChapterDownloaded(
                dbChapter.name,
                dbChapter.scanlator,
                dbChapter.url,
                /* SY --> */ manga.ogTitle /* SY <-- */,
                manga.source,
            )
        if (shouldRenameChapter) {
            downloadManager.renameChapter(source, manga, dbChapter, chapter)
        }
        val toChangeChapter = dbChapter.copy(
            name = chapter.name,
            chapterNumber = chapter.chapterNumber,
            scanlator = chapter.scanlator,
            sourceOrder = chapter.sourceOrder,
            memo = chapter.memo,
        )
        return if (chapter.dateUpload != 0L) toChangeChapter.copy(dateUpload = chapter.dateUpload) else toChangeChapter
    }

    // Stamps the new chapters with a descending fetch date and carries read/bookmark state over from
    // the removed chapters that share a chapter number (a re-uploaded chapter keeps its progress).
    private fun carryOverRemovedState(
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
    private fun carryOverEhProgress(
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
    private suspend fun persist(
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
}
