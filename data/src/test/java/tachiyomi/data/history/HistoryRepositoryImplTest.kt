package tachiyomi.data.history

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.data.InjektHarness
import tachiyomi.data.inMemoryDatabase
import tachiyomi.data.seedChapter
import tachiyomi.data.seedHistory
import tachiyomi.data.seedManga
import java.util.Date

/** The read side; the writes are in [HistoryRepositoryWriteTest]. */
internal class HistoryRepositoryImplTest {
    private val harness = InjektHarness()
    private val database = harness.database
    private val repository = HistoryRepositoryImpl(database)
    private var alphaId = 0L
    private var alphaChapterId = 0L

    @BeforeEach
    fun seed() = runTest {
        harness.install()
        // Alpha was read most recently; Beta earlier; Gamma never.
        alphaId = database.seedManga(title = "Alpha", thumbnailUrl = "thumb")
        alphaChapterId = database.seedChapter(mangaId = alphaId, name = "a1")
        database.seedHistory(alphaChapterId, readAt = Date(2000L), timeRead = 60L)
        val beta = database.seedManga(title = "Beta", favorite = false)
        database.seedHistory(database.seedChapter(mangaId = beta, name = "b1"), readAt = Date(1000L), timeRead = 40L)
        database.seedChapter(mangaId = database.seedManga(title = "Gamma"), name = "g1")
    }

    @AfterEach
    fun uninstall() = harness.uninstall()

    @Test
    fun historyListsLatestReadFirst() = runTest {
        val rows = repository.getHistory("").first()
        rows.map { it.title } shouldBe listOf("Alpha", "Beta")
        val alpha = rows.first()
        alpha.mangaId shouldBe alphaId
        alpha.chapterId shouldBe alphaChapterId
        alpha.readAt shouldBe Date(2000L)
        alpha.readDuration shouldBe 60L
        alpha.chapterNumber shouldBe 1.0
        alpha.coverData.url shouldBe "thumb"
        alpha.coverData.isMangaFavorite shouldBe true
    }

    @Test
    fun historyFiltersByTitle() = runTest {
        repository.getHistory("bet").first().map { it.title } shouldBe listOf("Beta")
        repository.getHistory("zzz").first() shouldBe emptyList()
    }

    @Test
    fun lastHistoryIsTheLatestRead() = runTest {
        val last = repository.getLastHistory()
        last?.mangaId shouldBe alphaId
        last?.coverData?.ogUrl shouldBe "thumb"
    }

    @Test
    fun lastHistoryNullWithoutReads() = runTest {
        HistoryRepositoryImpl(inMemoryDatabase()).getLastHistory() shouldBe null
    }

    @Test
    fun totalReadDurationSumsRows() = runTest {
        repository.getTotalReadDuration() shouldBe 100L
        HistoryRepositoryImpl(inMemoryDatabase()).getTotalReadDuration() shouldBe 0L
    }

    @Test
    fun historyByMangaId() = runTest {
        val rows = repository.getHistoryByMangaId(alphaId)
        rows.map { it.chapterId } shouldBe listOf(alphaChapterId)
        rows.single().readAt shouldBe Date(2000L)
        rows.single().readDuration shouldBe 60L
        repository.getHistoryByMangaId(999L) shouldBe emptyList()
    }
}
