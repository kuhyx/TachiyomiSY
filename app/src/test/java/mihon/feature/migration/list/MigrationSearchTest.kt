package mihon.feature.migration.list

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import mihon.domain.source.models.RemoteMangaUpdate
import mihon.feature.migration.list.models.MigratingManga.SearchResult
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.domain.manga.model.Manga

internal class MigrationSearchTest {
    private val harness = MigrationListHarness()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        harness.start()
        harness.preferences.migrationSources.set(listOf(10L))
    }

    @AfterEach
    fun tearDown() {
        harness.stop()
        Dispatchers.resetMain()
    }

    private fun stubDetails(block: (Manga) -> Result<RemoteMangaUpdate>) {
        coEvery {
            harness.updateMangaFromRemote(
                manga = any(),
                fetchDetails = any(),
                fetchChapters = any(),
                manualFetch = any(),
                fetchWindow = any(),
                throttleFunc = any(),
            )
        } answers { block(firstArg()) }
    }

    @Test
    fun aFailingSourceIsNotFound() {
        harness.hits[10L] = { error("down") }
        val state = harness.model(listOf(1L)).awaitState { it.finishedCount == 1 }
        state.items.single().searchResult.value shouldBe SearchResult.NotFound
    }

    @Test
    fun aFailedRefreshStillMatches() {
        harness.hits[10L] = { listOf(hit("Entry 1", id = 51L)) }
        stubDetails { Result.failure(IllegalStateException("refresh")) }
        val state = harness.model(listOf(1L)).awaitState { it.finishedCount == 1 }
        state.items.single().searchResult.value.shouldBeInstanceOf<SearchResult.Success>()
    }

    @Test
    fun coverlessMatchFetchesDetails() {
        harness.hits[10L] = { listOf(hit("Entry 1", id = 51L, thumbnail = null)) }
        val state = harness.model(listOf(1L)).awaitState { it.finishedCount == 1 }
        state.items.single().searchResult.value.shouldBeInstanceOf<SearchResult.Success>()
        coVerify {
            harness.updateMangaFromRemote(
                manga = any(),
                fetchDetails = true,
                fetchChapters = any(),
                manualFetch = true,
                fetchWindow = any(),
                throttleFunc = any(),
            )
        }
    }

    @Test
    fun aFailedCoverFetchIsQuiet() {
        harness.hits[10L] = { listOf(hit("Entry 1", id = 51L, thumbnail = null)) }
        stubDetails { throw IllegalStateException("cover") }
        val state = harness.model(listOf(1L)).awaitState { it.finishedCount == 1 }
        state.items.single().searchResult.value.shouldBeInstanceOf<SearchResult.Success>()
    }

    @Test
    fun aCancelledCoverFetchStops() {
        harness.hits[10L] = { listOf(hit("Entry 1", id = 51L, thumbnail = null)) }
        var calls = 0
        stubDetails {
            calls++
            if (calls > 1) throw CancellationException("stop") else Result.success(RemoteMangaUpdate(it, emptyList()))
        }
        val model = harness.model(listOf(1L))
        runBlocking { withTimeout(WAIT_MS) { while (calls < 2) delay(10) } }
        Thread.sleep(200)
        model.items.single().searchResult.value shouldBe SearchResult.Searching
    }

    @Test
    fun aCancelledSearchStops() {
        harness.hits[10L] = { throw CancellationException("stop") }
        val model = harness.model(listOf(1L, 2L))
        model.awaitState { it.items.size == 2 }
        Thread.sleep(300)
        model.items.first().searchResult.value shouldBe SearchResult.Searching
    }

    @Test
    fun removedEntriesAreNotSearched() {
        val gate = CompletableDeferred<Unit>()
        harness.hits[10L] = { query ->
            if (query == "Entry 1") runBlocking { gate.await() }
            emptyList()
        }
        val model = harness.model(listOf(1L, 2L, 3L))
        model.awaitState { it.items.size == 3 }
        model.items[1].cancelMigration()
        model.removeManga(3L)
        model.awaitState { it.items.size == 2 }
        gate.complete(Unit)
        model.awaitState { it.finishedCount == 1 }
        model.items[1].searchResult.value shouldBe SearchResult.Searching
    }

    @Test
    fun disposingStopsTheRun() {
        val gate = CompletableDeferred<Unit>()
        harness.hits[10L] = {
            runBlocking { gate.await() }
            emptyList()
        }
        val model = harness.model(listOf(1L, 2L))
        model.awaitState { it.items.size == 2 }
        model.onDispose()
        gate.complete(Unit)
        Thread.sleep(300)
        model.items.all { it.searchResult.value == SearchResult.Searching } shouldBe true
    }
}
