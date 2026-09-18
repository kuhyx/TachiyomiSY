package tachiyomi.domain.history.interactor

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.domain.history.model.CustomMangaInfoScope
import tachiyomi.domain.history.model.History
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.domain.history.repository.HistoryRepository
import tachiyomi.domain.manga.model.MangaCover
import java.util.Date

internal class HistoryInteractorsTest {

    private val repository: HistoryRepository = mockk()

    @BeforeEach
    fun beforeEach() {
        CustomMangaInfoScope.install()
    }

    @AfterEach
    fun afterEach() {
        CustomMangaInfoScope.restore()
    }

    @Test
    fun getHistoryAwaitByManga() = runTest {
        val rows = listOf(History.create().copy(id = 1L))
        coEvery { repository.getHistoryByMangaId(5L) } returns rows

        GetHistory(repository).await(5L) shouldContainExactly rows
    }

    @Test
    fun getHistorySubscribeQuery() = runTest {
        val rows = listOf(historyWithRelations())
        every { repository.getHistory("naru") } returns flowOf(rows)

        GetHistory(repository).subscribe("naru").first() shouldContainExactly rows
    }

    @Test
    fun totalReadDuration() = runTest {
        coEvery { repository.getTotalReadDuration() } returns 1_234L

        GetTotalReadDuration(repository).await() shouldBe 1_234L
    }

    @Test
    fun removeAllDelegates() = runTest {
        coEvery { repository.deleteAllHistory() } returns true

        RemoveHistory(repository).awaitAll() shouldBe true
    }

    @Test
    fun removeEntryResetsById() = runTest {
        coEvery { repository.resetHistory(1L) } returns Unit

        RemoveHistory(repository).await(historyWithRelations())

        coVerify(exactly = 1) { repository.resetHistory(1L) }
    }

    @Test
    fun removeByMangaResets() = runTest {
        coEvery { repository.resetHistoryByMangaId(7L) } returns Unit

        RemoveHistory(repository).await(7L)

        coVerify(exactly = 1) { repository.resetHistoryByMangaId(7L) }
    }

    @Test
    fun removeByHistoryIdResets() = runTest {
        coEvery { repository.resetHistory(9L) } returns Unit

        RemoveHistory(repository).awaitById(9L)

        coVerify(exactly = 1) { repository.resetHistory(9L) }
    }
}

private fun historyWithRelations(): HistoryWithRelations = HistoryWithRelations(
    id = 1L,
    chapterId = 2L,
    mangaId = CustomMangaInfoScope.PLAIN_MANGA_ID,
    ogTitle = "Naruto",
    chapterNumber = 1.0,
    readAt = Date(0L),
    readDuration = 0L,
    coverData = MangaCover(
        mangaId = CustomMangaInfoScope.PLAIN_MANGA_ID,
        sourceId = 1L,
        isMangaFavorite = false,
        ogUrl = null,
        lastModified = 0L,
    ),
)
