package mihon.feature.migration.list

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** Which search results count as a match, called directly rather than through a whole migration. */
internal class MigrationSearchPickTest {
    private val harness = MigrationListHarness()
    private val entry = migrating(1L).manga.copy(url = "/manga/51")

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        harness.start()
        harness.hits[1L] = { listOf(hit("Entry 1", id = 51L)) }
        harness.hits[10L] = { listOf(hit("Entry 1", id = 51L)) }
    }

    @AfterEach
    fun tearDown() {
        harness.stop()
        Dispatchers.resetMain()
    }

    @Test
    fun theEntryItselfIsNoMatch() {
        val model = harness.model(emptyList())
        runBlocking { model.searchSource(entry, harness.searchSource(1L), deepSearchMode = false) }.shouldBeNull()
        val other = runBlocking { model.searchSource(entry, harness.searchSource(10L), deepSearchMode = false) }
        other?.first?.id shouldBe 51L
    }

    @Test
    fun chapterlessMatchesAreSkipped() {
        harness.hits[11L] = { listOf(hit("Entry 1", id = 61L)) }
        harness.chapters[61L] = listOf(1.0, 2.0)
        val model = harness.model(emptyList())
        val sources = listOf(10L, 11L, 12L).map(harness::searchSource)
        val best = runBlocking { model.searchByMostChapters(migrating(1L), sources, deepSearchMode = false) }
        best?.first?.id shouldBe 61L
    }

    @Test
    fun noSourcesFindNothing() {
        val model = harness.model(emptyList())
        runBlocking { model.searchFirstMatch(migrating(1L), emptyList(), deepSearchMode = false) }.shouldBeNull()
    }
}
