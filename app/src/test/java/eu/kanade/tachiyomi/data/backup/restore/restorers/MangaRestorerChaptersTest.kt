package eu.kanade.tachiyomi.data.backup.restore.restorers

import eu.kanade.tachiyomi.data.backup.models.BackupChapter
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter

internal class MangaRestorerChaptersTest {

    private val harness = MangaRestorerHarness()
    private val manga by lazy { dbManga() }

    @BeforeEach
    fun setUp() = harness.start()

    @AfterEach
    fun tearDown() = harness.stop()

    private fun stored(vararg chapters: Chapter) {
        coEvery { harness.graph.getChaptersByMangaId.await(1L, any()) } returns chapters.toList()
    }

    private fun verifyUpdated(
        id: Long,
        read: Boolean,
        lastPageRead: Long,
        sourceOrder: Long?,
        times: Int = -1,
    ) = coVerify(exactly = times) {
        harness.graph.chapters.update(
            mangaId = null,
            url = null,
            name = null,
            scanlator = null,
            read = read,
            bookmark = any(),
            lastPageRead = lastPageRead,
            chapterNumber = null,
            sourceOrder = sourceOrder,
            dateFetch = null,
            dateUpload = null,
            chapterId = id,
            version = any(),
            isSyncing = 1L,
            memo = any(),
        )
    }

    @Test
    fun newChaptersAreInserted() = runTest {
        harness.restorer().restoreChapters(manga, listOf(BackupChapter(url = "/new", name = "New")))
        coVerify {
            harness.graph.chapters.insert(
                mangaId = 1L,
                url = "/new",
                name = "New",
                scanlator = null,
                read = false,
                bookmark = false,
                lastPageRead = 0L,
                chapterNumber = 0.0,
                sourceOrder = 0L,
                dateFetch = 0L,
                dateUpload = 0L,
                version = 0L,
                memo = any(),
            )
        }
    }

    @Test
    fun unchangedChaptersAreSkipped() = runTest {
        stored(dbChapter(id = 5L, url = "/same", version = 3))
        val backup = listOf(BackupChapter(url = "/same", name = "/same", version = 4))
        harness.restorer().restoreChapters(manga, backup)
        stored(dbChapter(id = 5L, url = "/same", version = 4))
        harness.restorer(isSync = true).restoreChapters(manga, backup)
        verifyUpdated(id = 5L, read = false, lastPageRead = 0L, sourceOrder = 0L, times = 0)
        coVerify(exactly = 0) {
            harness.graph.chapters.insert(
                mangaId = any(),
                url = any(),
                name = any(),
                scanlator = any(),
                read = any(),
                bookmark = any(),
                lastPageRead = any(),
                chapterNumber = any(),
                sourceOrder = any(),
                dateFetch = any(),
                dateUpload = any(),
                version = any(),
                memo = any(),
            )
        }
    }

    @Test
    fun syncTakesANewerVersion() = runTest {
        stored(dbChapter(id = 5L, url = "/same", version = 3))
        val backup = BackupChapter(url = "/same", name = "/same", version = 4)
        harness.restorer(isSync = true).restoreChapters(manga, listOf(backup))
        verifyUpdated(id = 5L, read = false, lastPageRead = 0L, sourceOrder = 0L)
    }

    @Test
    fun syncOverwritesProgress() = runTest {
        stored(
            dbChapter(id = 5L, url = "/c", read = true, bookmark = true, lastPageRead = 9),
            dbChapter(id = 6L, url = "/e"),
            dbChapter(id = 7L, url = "/f"),
        )
        val restorer = harness.restorer(isSync = true)
        restorer.restoreChapters(
            manga,
            listOf(
                BackupChapter(url = "/c", name = "/c", read = false, lastPageRead = 2, sourceOrder = 1),
                BackupChapter(url = "/e", name = "/e", read = true, bookmark = true, sourceOrder = 2),
                BackupChapter(url = "/f", name = "/f", lastPageRead = 1),
            ),
        )
        verifyUpdated(id = 5L, read = false, lastPageRead = 2L, sourceOrder = 1L)
        verifyUpdated(id = 6L, read = true, lastPageRead = 0L, sourceOrder = 2L)
        verifyUpdated(id = 7L, read = false, lastPageRead = 1L, sourceOrder = 0L)
        val stored = dbChapter(id = 5L, url = "/c", bookmark = true)
        val merged = restorer.updateChapterBasedOnSyncState(BackupChapter("/c", "/c").toChapterImpl(), stored)
        merged.bookmark shouldBe true
    }

    // kuhyx/TachiyomiSY#33: a changed chapter keeps the backup's id (-1), so a plain restore
    // re-inserts it instead of updating the stored row. These pin that until the fix lands.

    @Test
    fun restoreKeepsLocalReadState() = runTest {
        stored(
            dbChapter(id = 5L, url = "/c", read = true, lastPageRead = 9),
            dbChapter(id = 6L, url = "/d", read = true, lastPageRead = 2),
        )
        harness.restorer().restoreChapters(
            manga,
            listOf(
                BackupChapter(url = "/c", name = "renamed", read = false),
                BackupChapter(url = "/d", name = "renamed", read = true),
            ),
        )
        verifyReinserted(url = "/c", read = true, lastPageRead = 9L)
        verifyReinserted(url = "/d", read = true, lastPageRead = 2L)
    }

    @Test
    fun restoreKeepsLocalPage() = runTest {
        stored(dbChapter(id = 5L, url = "/c", lastPageRead = 9), dbChapter(id = 6L, url = "/e"))
        harness.restorer().restoreChapters(
            manga,
            listOf(BackupChapter(url = "/c", name = "renamed"), BackupChapter(url = "/e", name = "renamed")),
        )
        verifyReinserted(url = "/c", read = false, lastPageRead = 9L)
        verifyReinserted(url = "/e", read = false, lastPageRead = 0L)
    }

    @Test
    fun restoreTakesBackupProgress() = runTest {
        stored(dbChapter(id = 5L, url = "/c"), dbChapter(id = 6L, url = "/d", lastPageRead = 4))
        harness.restorer().restoreChapters(
            manga,
            listOf(
                BackupChapter(url = "/c", name = "renamed", read = true, lastPageRead = 3),
                BackupChapter(url = "/d", name = "renamed", lastPageRead = 8),
            ),
        )
        verifyReinserted(url = "/c", read = true, lastPageRead = 3L)
        verifyReinserted(url = "/d", read = false, lastPageRead = 8L)
    }

    private fun verifyReinserted(url: String, read: Boolean, lastPageRead: Long) = coVerify {
        harness.graph.chapters.insert(
            mangaId = 1L,
            url = url,
            name = url,
            scanlator = null,
            read = read,
            bookmark = false,
            lastPageRead = lastPageRead,
            chapterNumber = any(),
            sourceOrder = any(),
            dateFetch = any(),
            dateUpload = any(),
            version = any(),
            memo = any(),
        )
    }
}
