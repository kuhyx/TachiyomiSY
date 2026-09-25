package eu.kanade.tachiyomi.data.backup.restore.restorers

import eu.kanade.tachiyomi.data.backup.chapterRow
import eu.kanade.tachiyomi.data.backup.fakeQuery
import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import eu.kanade.tachiyomi.data.backup.models.BackupHistory
import eu.kanade.tachiyomi.data.backup.models.BackupTracking
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.data.History
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.track.model.Track
import java.util.Date

internal class MangaRestorerDetailsTest {

    private val harness = MangaRestorerHarness()
    private val manga by lazy { dbManga() }

    @BeforeEach
    fun setUp() = harness.start()

    @AfterEach
    fun tearDown() = harness.stop()

    private fun track(trackerId: Long, id: Long = 9L, remoteId: Long = 0L, lastRead: Double = 0.0) = Track(
        id = id,
        mangaId = 1L,
        trackerId = trackerId,
        remoteId = remoteId,
        libraryId = 0L,
        title = "",
        lastChapterRead = lastRead,
        totalChapters = 0L,
        status = 0L,
        score = 0.0,
        remoteUrl = "",
        startDate = 0L,
        finishDate = 0L,
        private = false,
    )

    @Test
    fun categoriesMatchByName() = runTest {
        val stored = Category(id = 3L, name = "A", order = 0, flags = 0)
        coEvery { harness.graph.getCategories.await() } returns listOf(stored)
        val backup = listOf(BackupCategory(name = "A", order = 1), BackupCategory(name = "Gone", order = 2))
        harness.restorer().restoreCategories(manga, categories = listOf(1L, 2L, 7L), backupCategories = backup)
        coVerify { harness.mangaCategories.deleteMangaCategoryByMangaId(1L) }
        coVerify(exactly = 1) { harness.mangaCategories.insert(any(), any()) }
        coVerify { harness.mangaCategories.insert(1L, 3L) }
    }

    @Test
    fun unknownCategoriesChangeNothing() = runTest {
        harness.restorer().restoreCategories(manga, categories = listOf(1L), backupCategories = emptyList())
        coVerify(exactly = 0) { harness.mangaCategories.deleteMangaCategoryByMangaId(any()) }
    }

    @Test
    fun newHistoryNeedsItsChapter() = runTest {
        every { harness.graph.chapters.getChapterByUrl("/c") } returns
            fakeQuery(listOf(chapterRow(id = 4L, mangaId = 2L), chapterRow(id = 5L, mangaId = 1L)))
        harness.restorer().restoreHistory(
            manga,
            listOf(BackupHistory(url = "/c", lastRead = 1_000L, readDuration = 30L), BackupHistory("/gone", 1L)),
        )
        coVerify(exactly = 1) { harness.history.upsert(any(), any(), any()) }
        coVerify { harness.history.upsert(5L, Date(1_000L), 30L) }
    }

    @Test
    fun storedHistoryKeepsTheLatest() = runTest {
        every { harness.history.getHistoryByChapterUrl(1L, "/newer") } returns
            fakeQuery(listOf(History(_id = 1L, chapter_id = 11L, last_read = Date(50L), time_read = 10L)))
        every { harness.history.getHistoryByChapterUrl(1L, "/unread") } returns
            fakeQuery(listOf(History(_id = 2L, chapter_id = 12L, last_read = null, time_read = 40L)))
        harness.restorer().restoreHistory(
            manga,
            listOf(BackupHistory("/newer", lastRead = 20L, readDuration = 25L), BackupHistory("/unread", 0L, 5L)),
        )
        coVerify { harness.history.upsert(11L, Date(50L), 15L) }
        coVerify { harness.history.upsert(12L, null, 0L) }
    }

    @Test
    fun emptyHistoryWritesNothing() = runTest {
        harness.restorer().restoreHistory(manga, listOf(BackupHistory("/gone", 1L)))
        coVerify(exactly = 0) { harness.history.upsert(any(), any(), any()) }
    }

    @Test
    fun tracksAreAddedOrUpdated() = runTest {
        coEvery { harness.graph.getTracks.await(1L) } returns listOf(track(trackerId = 1L), track(2L, id = 8L))
        harness.restorer().restoreTracking(
            manga,
            listOf(
                BackupTracking(syncId = 1, libraryId = 0L),
                BackupTracking(syncId = 2, libraryId = 5L, mediaId = 77L, lastChapterRead = 3F),
                BackupTracking(syncId = 3, libraryId = 0L),
            ),
        )
        coVerify { harness.graph.insertTrack.awaitAll(listOf(track(trackerId = 3L, id = 0L).copy(libraryId = 0L))) }
        coVerify(exactly = 1) {
            harness.mangaSync.update(1L, 2L, 77L, 5L, "", 3.0, 0L, 0L, 0.0, "", 0L, 0L, false, 8L)
        }
    }

    @Test
    fun unchangedTracksWriteNothing() = runTest {
        coEvery { harness.graph.getTracks.await(1L) } returns listOf(track(trackerId = 1L))
        harness.restorer().restoreTracking(manga, listOf(BackupTracking(syncId = 1, libraryId = 0L)))
        coVerify(exactly = 0) { harness.graph.insertTrack.awaitAll(any()) }
    }

    @Test
    fun onlyNewScanlatorsAreExcluded() = runTest {
        every { harness.excluded.getExcludedScanlatorsByMangaId(1L) } returns fakeQuery(listOf("old"))
        val restorer = harness.restorer()
        restorer.restoreExcludedScanlators(manga, emptyList())
        restorer.restoreExcludedScanlators(manga, listOf("old"))
        coVerify(exactly = 0) { harness.excluded.insert(any(), any()) }
        restorer.restoreExcludedScanlators(manga, listOf("old", "new"))
        coVerify(exactly = 1) { harness.excluded.insert(any(), any()) }
        coVerify { harness.excluded.insert(1L, "new") }
    }
}
