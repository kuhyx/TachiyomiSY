package mihon.feature.migration.list

import cafe.adriel.voyager.core.model.screenModelScope
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import mihon.feature.migration.list.models.MigratingManga.SearchResult
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CopyOnWriteArrayList

/** How the search loop walks the list: it stops once cancelled and skips entries already decided. */
internal class MigrationListRunTest {
    private val harness = MigrationListHarness()
    private val built = CompletableDeferred<MigrationListScreenModel>()
    private val queries = CopyOnWriteArrayList<String>()

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

    // The search runs on a worker thread, so it may start before the constructor has returned.
    private fun builtModel(): MigrationListScreenModel = runBlocking { withTimeout(WAIT_MS) { built.await() } }

    private fun start(): MigrationListScreenModel = harness.model(listOf(1L, 2L)).also { built.complete(it) }

    @Test
    fun aCancelledRunStopsEarly() {
        // Models built outside a Navigator share one screenModelScope, so its jobs are cancelled, not the scope.
        val cancelled = CompletableDeferred<List<Job>>()
        harness.hits[10L] = { query ->
            queries += query
            if (query == "Entry 1") {
                val scopeJob = builtModel().screenModelScope.coroutineContext.job
                val jobs = scopeJob.children.toList()
                jobs.forEach { it.cancel() }
                cancelled.complete(jobs)
            }
            emptyList()
        }
        val model = start()
        runBlocking { withTimeout(WAIT_MS) { cancelled.await().joinAll() } }
        queries.none { "Entry 2" in it } shouldBe true
        model.items[1].searchResult.value shouldBe SearchResult.Searching
    }

    @Test
    fun aManualPickIsNotSearched() {
        harness.hits[10L] = { query ->
            queries += query
            if (query == "Entry 1") {
                val model = builtModel()
                model.useMangaForMigration(current = 2L, target = 52L) {}
                // The pick lands before the loop reaches the second entry.
                runBlocking {
                    withTimeout(WAIT_MS) { model.items[1].searchResult.first { it is SearchResult.Success } }
                }
            }
            emptyList()
        }
        val state = start().awaitState { it.finishedCount == 2 }
        queries.none { "Entry 2" in it } shouldBe true
        state.items[1].searchResult.value.shouldBeInstanceOf<SearchResult.Success>().manga.id shouldBe 52L
    }

    @Test
    fun aDeepFirstMatch() {
        harness.preferences.migrationDeepSearchMode.set(true)
        harness.hits[10L] = { listOf(hit("Entry 1", id = 51L)) }
        val state = harness.model(listOf(1L)).awaitState { it.finishedCount == 1 }
        state.items.single().searchResult.value.shouldBeInstanceOf<SearchResult.Success>().manga.id shouldBe 51L
    }
}
