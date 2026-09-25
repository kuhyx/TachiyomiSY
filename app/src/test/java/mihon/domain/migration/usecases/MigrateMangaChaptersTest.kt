package mihon.domain.migration.usecases

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import mihon.domain.migration.models.MigrationFlag
import mihon.domain.migration.usecases.MigrateMangaHarness.Companion.current
import mihon.domain.migration.usecases.MigrateMangaHarness.Companion.target
import org.junit.jupiter.api.Test
import tachiyomi.domain.history.model.History
import tachiyomi.domain.history.model.HistoryUpdate
import java.util.Date

/** The chapter half of [MigrateMangaUseCase]: progress, bookmarks and history carried over by chapter number. */
internal class MigrateMangaChaptersTest {

    private val harness = MigrateMangaHarness()
    private val useCase = harness.useCase

    @Test
    fun carriesProgressOverByNumber() = runTest {
        harness.stubHappyPath()
        harness.sourcePreferences.migrationFlags.set(setOf(MigrationFlag.CHAPTER))
        coEvery { harness.getChaptersByMangaId.await(10) } returns listOf(
            migratedChapter(
                id = 1,
                mangaId = 10,
                number = 1.0,
                read = true,
                bookmark = true,
                lastPageRead = 4,
                dateFetch = 50,
            ),
            migratedChapter(id = 2, mangaId = 10, number = 2.0, read = true, dateFetch = 60),
            migratedChapter(id = 3, mangaId = 10, number = 3.0, lastPageRead = 2, dateFetch = 70),
            migratedChapter(id = 4, mangaId = 10, number = -1.0, read = true),
            migratedChapter(id = 5, mangaId = 10, number = 4.0),
        )
        coEvery { harness.getChaptersByMangaId.await(20) } returns listOf(
            migratedChapter(id = 11, mangaId = 20, number = 1.0),
            migratedChapter(id = 12, mangaId = 20, number = 2.0, read = true),
            migratedChapter(id = 13, mangaId = 20, number = 3.0),
            migratedChapter(id = 14, mangaId = 20, number = 1.5),
            migratedChapter(id = 15, mangaId = 20, number = -1.0),
            migratedChapter(id = 16, mangaId = 20, number = 4.0, read = true),
        )
        coEvery { harness.getHistoryByMangaId.await(10) } returns listOf(
            History(id = 1, chapterId = 1, readAt = Date(5_000), readDuration = 30),
            History(id = 2, chapterId = 2, readAt = null, readDuration = 10),
            History(id = 4, chapterId = 5, readAt = Date(7_000), readDuration = 1),
        )
        coEvery { harness.getHistoryByMangaId.await(20) } returns listOf(
            History(id = 3, chapterId = 12, readAt = Date(9_000), readDuration = 5),
        )
        useCase(current, target, replace = false)
        val updates = harness.chapterUpdates.single().associateBy { it.id }
        updates.getValue(11).let {
            it.read shouldBe true
            it.bookmark shouldBe true
            it.lastPageRead shouldBe 4L
            it.dateFetch shouldBe 50L
        }
        updates.getValue(12).read shouldBe true
        updates.getValue(13).let {
            it.read shouldBe false
            it.lastPageRead shouldBe 2L
            it.dateFetch shouldBe 70L
        }
        updates.getValue(14).read shouldBe true
        updates.getValue(15).read shouldBe false
        updates.getValue(16).read shouldBe true
        harness.historyUpdates.single() shouldBe listOf(
            HistoryUpdate(chapterId = 11, readAt = Date(5_000), sessionReadDuration = 30),
            HistoryUpdate(chapterId = 16, readAt = Date(7_000), sessionReadDuration = 1),
        )
    }

    @Test
    fun nothingReadMeansNothingMarked() = runTest {
        harness.stubHappyPath()
        harness.sourcePreferences.migrationFlags.set(setOf(MigrationFlag.CHAPTER))
        coEvery { harness.getChaptersByMangaId.await(10) } returns listOf(
            migratedChapter(id = 1, mangaId = 10, number = 1.0),
        )
        coEvery { harness.getChaptersByMangaId.await(20) } returns listOf(
            migratedChapter(id = 11, mangaId = 20, number = 1.0, read = true),
            migratedChapter(id = 12, mangaId = 20, number = 2.0),
        )
        useCase(current, target, replace = false)
        harness.chapterUpdates.single().map { it.read } shouldBe listOf(true, false)
        coVerify(exactly = 1) { harness.updateHistory.awaitAll(emptyList()) }
    }
}
