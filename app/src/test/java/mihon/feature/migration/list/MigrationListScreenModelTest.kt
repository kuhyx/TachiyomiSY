package mihon.feature.migration.list

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import mihon.feature.migration.list.models.MigratingManga.SearchResult
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class MigrationListScreenModelTest {
    private val harness = MigrationListHarness()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        harness.start()
        harness.preferences.migrationSources.set(listOf(10L))
    }

    @AfterEach
    fun tearDown() {
        harness.stop()
        Dispatchers.resetMain()
    }

    @Test
    fun theFirstMatchIsTaken() {
        harness.hits[10L] = { query -> if (query == "Entry 1") listOf(hit("Entry 1", id = 51L)) else emptyList() }
        harness.chapters[51L] = listOf(1.0, 2.0)
        val model = harness.model(listOf(1L, 2L, 150L))
        val state = model.awaitState { it.finishedCount == 2 }
        state.items.map { it.manga.id } shouldContainExactly listOf(1L, 2L)
        state.mangaIds shouldContainExactly listOf(1L, 2L)
        state.migrationComplete shouldBe true
        val match = state.items[0].searchResult.value.shouldBeInstanceOf<SearchResult.Success>()
        match.manga.id shouldBe 51L
        match.chapterCount shouldBe 2
        match.latestChapter shouldBe 2.0
        match.source shouldBe "Source 10"
        state.items[1].searchResult.value shouldBe SearchResult.NotFound
        state.items[0].source shouldBe "Source 1"
    }

    @Test
    fun theMostChaptersWin() {
        harness.preferences.migrationSources.set(listOf(10L, 11L, 12L))
        harness.preferences.migrationPrioritizeByChapters.set(true)
        harness.preferences.migrationDeepSearchMode.set(true)
        harness.hits[10L] = { listOf(hit("Entry 1", id = 51L)) }
        harness.hits[11L] = { listOf(hit("Entry 1", id = 52L)) }
        harness.hits[12L] = { listOf(hit("Entry 1", id = 53L)) }
        harness.chapters[51L] = listOf(1.0)
        harness.chapters[52L] = listOf(1.0, 5.0)
        val model = harness.model(listOf(1L), extraQuery = "lang:en")
        val state = model.awaitState { it.finishedCount == 1 }
        state.items.single().searchResult.value.shouldBeInstanceOf<SearchResult.Success>().manga.id shouldBe 52L
    }

    @Test
    fun noChaptersAnywhereIsNotFound() {
        harness.preferences.migrationPrioritizeByChapters.set(true)
        harness.hits[10L] = { listOf(hit("Entry 1", id = 51L)) }
        val state = harness.model(listOf(1L)).awaitState { it.finishedCount == 1 }
        state.items.single().searchResult.value shouldBe SearchResult.NotFound
        state.migrationComplete shouldBe false
    }

    @Test
    fun theSameEntryIsNotAMatch() {
        harness.preferences.migrationSources.set(listOf(1L))
        harness.hits[1L] = { listOf(hit("Entry 1", id = 1L).apply { url = "" }) }
        val state = harness.model(listOf(1L)).awaitState { it.finishedCount == 1 }
        state.items.single().searchResult.value shouldBe SearchResult.NotFound
    }

    @Test
    fun unknownSourcesAreSkipped() {
        harness.preferences.migrationSources.set(listOf(10L, 99L))
        io.mockk.every { harness.sourceManager.get(99L) } returns null
        harness.hits[10L] = { listOf(hit("Entry 1", id = 51L)) }
        harness.model(listOf(1L)).awaitState { it.finishedCount == 1 }.migrationComplete shouldBe true
    }

    @Test
    fun unmatchedCanBeHidden() {
        harness.preferences.migrationHideUnmatched.set(true)
        val model = harness.model(listOf(1L))
        runBlocking { withTimeout(WAIT_MS) { model.navigateBackEvent.first() } }
        model.items shouldBe emptyList()
    }

    @Test
    fun noNewChaptersCanBeHidden() {
        harness.preferences.migrationHideWithoutUpdates.set(true)
        harness.chapters[1L] = listOf(4.0)
        harness.chapters[2L] = listOf(4.0)
        harness.chapters[51L] = listOf(2.0)
        harness.chapters[52L] = listOf(9.0)
        harness.hits[10L] = { query ->
            if (query == "Entry 1") listOf(hit("Entry 1", id = 51L)) else listOf(hit("Entry 2", id = 52L))
        }
        val model = harness.model(listOf(1L, 2L))
        val state = model.awaitState { it.items.size == 1 && it.finishedCount == 1 }
        state.items.single().manga.id shouldBe 2L
    }

    @Test
    fun missingChapterNumbersCountAsZero() {
        harness.preferences.migrationHideWithoutUpdates.set(true)
        harness.hits[10L] = { listOf(hit("Entry 1", id = 51L)) }
        val model = harness.model(listOf(1L))
        runBlocking { withTimeout(WAIT_MS) { model.navigateBackEvent.first() } }
    }
}
