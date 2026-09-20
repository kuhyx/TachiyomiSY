package eu.kanade.tachiyomi.data.backup.restore.restorers

import eu.kanade.tachiyomi.data.backup.models.BackupChapter
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.model.copyFrom
import tachiyomi.domain.manga.model.Manga
import uy.kohesive.injekt.api.get

internal suspend fun MangaRestorer.restoreChapters(manga: Manga, backupChapters: List<BackupChapter>) {
    val dbChaptersByUrl = getChaptersByMangaId.await(manga.id)
        .associateBy { it.url }

    val (existingChapters, newChapters) = backupChapters
        .mapNotNull { backupChapter ->
            val chapter = backupChapter.toChapterImpl().copy(mangaId = manga.id)
            val dbChapter = dbChaptersByUrl[chapter.url]

            when {
                dbChapter == null -> {
                    chapter // New chapter
                }
                chapter.forComparison() == dbChapter.forComparison() -> {
                    if (isSync && chapter.version != dbChapter.version) {
                        chapter.copy(id = dbChapter.id)
                    } else {
                        null // Same state; skip
                    }
                }
                else -> {
                    updateChapterBasedOnSyncState(chapter, dbChapter)
                }
            }
        }
        .partition { it.id > 0 }

    insertNewChapters(newChapters)
    updateExistingChapters(existingChapters)
}

internal fun MangaRestorer.updateChapterBasedOnSyncState(chapter: Chapter, dbChapter: Chapter): Chapter {
    return if (isSync) {
        chapter.copy(
            id = dbChapter.id,
            bookmark = chapter.bookmark || dbChapter.bookmark,
            read = chapter.read,
            lastPageRead = chapter.lastPageRead,
            sourceOrder = chapter.sourceOrder,
        )
    } else {
        chapter.copyFrom(dbChapter).let {
            when {
                dbChapter.read && !it.read -> it.copy(read = true, lastPageRead = dbChapter.lastPageRead)
                it.lastPageRead == 0L && dbChapter.lastPageRead != 0L -> it.copy(
                    lastPageRead = dbChapter.lastPageRead,
                )
                else -> it
            }
        }
    }
}

internal suspend fun MangaRestorer.insertNewChapters(chapters: List<Chapter>) {
    database.transaction {
        chapters.forEach { chapter ->
            database.chaptersQueries.insert(
                chapter.mangaId,
                chapter.url,
                chapter.name,
                chapter.scanlator,
                chapter.read,
                chapter.bookmark,
                chapter.lastPageRead,
                chapter.chapterNumber,
                chapter.sourceOrder,
                chapter.dateFetch,
                chapter.dateUpload,
                chapter.version,
                chapter.memo,
            )
        }
    }
}

internal suspend fun MangaRestorer.updateExistingChapters(chapters: List<Chapter>) {
    database.transaction {
        chapters.forEach { chapter ->
            database.chaptersQueries.update(
                mangaId = null,
                url = null,
                name = null,
                scanlator = null,
                read = chapter.read,
                bookmark = chapter.bookmark,
                lastPageRead = chapter.lastPageRead,
                chapterNumber = null,
                sourceOrder = if (isSync) chapter.sourceOrder else null,
                dateFetch = null,
                dateUpload = null,
                chapterId = chapter.id,
                version = chapter.version,
                isSyncing = 1,
                memo = chapter.memo,
            )
        }
    }
}
