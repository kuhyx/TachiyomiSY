package tachiyomi.data.history

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.data.Database
import tachiyomi.data.HistoryQueries
import tachiyomi.data.inMemoryDatabase
import tachiyomi.data.seedChapter
import tachiyomi.data.seedHistory
import tachiyomi.data.seedManga
import tachiyomi.domain.history.model.History
import tachiyomi.domain.history.model.HistoryUpdate
import java.util.Date

/** The write side of [HistoryRepositoryImpl], including the failure paths of a store that throws. */
internal class HistoryRepositoryWriteTest {
    private val database = inMemoryDatabase()
    private val repository = HistoryRepositoryImpl(database)
    private val failingQueries = mockk<HistoryQueries> {
        coEvery { resetHistoryById(any()) } throws IllegalStateException("boom")
        coEvery { resetHistoryByMangaId(any()) } throws IllegalStateException("boom")
        coEvery { removeAllHistory() } throws IllegalStateException("boom")
    }
    private val failing = HistoryRepositoryImpl(mockk<Database> { every { historyQueries } returns failingQueries })
    private var mangaId = 0L
    private var firstChapter = 0L
    private var secondChapter = 0L

    @BeforeEach
    fun seed() = runTest {
        mangaId = database.seedManga(title = "Alpha")
        firstChapter = database.seedChapter(mangaId = mangaId, name = "1")
        secondChapter = database.seedChapter(mangaId = mangaId, name = "2")
        database.seedHistory(firstChapter, readAt = Date(2000L), timeRead = 60L)
    }

    private suspend fun history(): List<History> = repository.getHistoryByMangaId(mangaId).sortedBy { it.chapterId }

    @Test
    fun resetHistoryClearsReadTime() = runTest {
        repository.resetHistory(history().single().id)
        history().single().readAt shouldBe Date(0L)
    }

    @Test
    fun resetFailureIsSwallowed() = runTest {
        failing.resetHistory(1L)
    }

    @Test
    fun resetHistoryByMangaId() = runTest {
        repository.resetHistoryByMangaId(mangaId)
        history().single().readAt shouldBe Date(0L)
        history().single().readDuration shouldBe 60L
    }

    @Test
    fun resetByMangaFailureSwallowed() = runTest {
        failing.resetHistoryByMangaId(1L)
    }

    @Test
    fun deleteAllReportsOutcome() = runTest {
        repository.deleteAllHistory() shouldBe true
        history() shouldBe emptyList()
        failing.deleteAllHistory() shouldBe false
    }

    @Test
    fun upsertAddsSessionDuration() = runTest {
        val update = HistoryUpdate(chapterId = firstChapter, readAt = Date(3000L), sessionReadDuration = 10L)
        repository.upsertHistory(update)
        val row = history().single()
        row.readAt shouldBe Date(3000L)
        row.readDuration shouldBe 70L
    }

    @Test
    fun upsertAllInsertsAndUpdates() = runTest {
        repository.upsertAllHistory(
            listOf(
                HistoryUpdate(chapterId = firstChapter, readAt = Date(4000L), sessionReadDuration = 5L),
                HistoryUpdate(chapterId = secondChapter, readAt = Date(5000L), sessionReadDuration = 7L),
            ),
        )
        repository.upsertAllHistory(emptyList())
        history().map { it.readDuration } shouldBe listOf(65L, 7L)
        history().map { it.readAt } shouldBe listOf(Date(4000L), Date(5000L))
    }

    @Test
    fun upsertFailureIsSwallowed() = runTest {
        repository.upsertHistory(HistoryUpdate(chapterId = 999L, readAt = Date(1L), sessionReadDuration = 1L))
        history().single().readDuration shouldBe 60L
    }
}
