package mihon.feature.migration.list

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import mihon.feature.migration.list.MigrationListScreenModel.Dialog
import mihon.feature.migration.list.models.MigratingManga.SearchResult
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class MigrationActionsTest {
    private val harness = MigrationListHarness()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        harness.start()
        harness.preferences.migrationSources.set(listOf(10L))
        harness.hits[10L] = { query -> if (query == "Entry 1") listOf(hit("Entry 1", id = 51L)) else emptyList() }
    }

    @AfterEach
    fun tearDown() {
        harness.stop()
        Dispatchers.resetMain()
    }

    private fun settled(ids: List<Long> = listOf(1L, 2L)): MigrationListScreenModel {
        val model = harness.model(ids)
        model.awaitState { it.items.size == ids.size && it.finishedCount == ids.size }
        return model
    }

    private fun MigrationListScreenModel.awaitBack() = runBlocking {
        withTimeout(WAIT_MS) { navigateBackEvent.first() }
    }

    @Test
    fun dialogsOpenAndClose() {
        val model = settled()
        model.showMigrateDialog(copy = true)
        model.state.value.dialog shouldBe Dialog.Migrate(copy = true, totalCount = 2, skippedCount = 1)
        model.showExitDialog()
        model.state.value.dialog shouldBe Dialog.Exit
        model.dismissDialog()
        model.state.value.dialog shouldBe null
    }

    @Test
    fun migratingEveryMatch() {
        val model = settled()
        coEvery { harness.migrateManga(any(), any(), any(), any()) } throws IllegalStateException("nope")
        model.migrateMangas()
        model.awaitBack()
        coVerify {
            harness.migrateManga(current = match { it.id == 1L }, target = any(), replace = true, throttleFunc = any())
        }
        model.awaitState { it.dialog == null }
    }

    @Test
    fun copyingEveryMatch() {
        val model = settled()
        model.copyMangas()
        model.awaitBack()
        coVerify { harness.migrateManga(current = any(), target = any(), replace = false, throttleFunc = any()) }
    }

    @Test
    fun aMigrationCanBeCancelled() {
        val model = settled()
        val gate = CompletableDeferred<Unit>()
        coEvery { harness.migrateManga(any(), any(), any(), any()) } coAnswers { gate.await() }
        model.migrateMangas()
        model.awaitState { it.dialog is Dialog.Progress }
        model.cancelMigrate()
        model.awaitState { it.dialog == null }
        model.migrateJob shouldBe null
        model.cancelMigrate()
    }

    @Test
    fun migratingOneNow() {
        val model = settled()
        model.migrateNow(mangaId = 2L, replace = true)
        model.migrateNow(mangaId = 77L, replace = true)
        model.migrateNow(mangaId = 1L, replace = false)
        model.awaitState { it.items.size == 1 }.items.single().manga.id shouldBe 2L
        coVerify(exactly = 1) {
            harness.migrateManga(current = any(), target = any(), replace = false, throttleFunc = any())
        }
    }

    @Test
    fun removingTheLastGoesBack() {
        val model = settled(listOf(1L))
        model.removeManga(77L)
        model.removeManga(1L)
        model.awaitBack()
        model.items shouldBe emptyList()
    }

    @Test
    fun aManualMatchIsUsed() {
        val model = settled()
        var missing = 0
        model.useMangaForMigration(current = 2L, target = 3L) { missing++ }
        model.useMangaForMigration(current = 77L, target = 3L) { missing++ }
        val result = runBlocking {
            withTimeout(WAIT_MS) { model.items[1].searchResult.first { it is SearchResult.Success } }
        }
        result.shouldBeInstanceOf<SearchResult.Success>().manga.id shouldBe 3L
        missing shouldBe 0
    }

    @Test
    fun aManualMatchWithoutEntry() {
        val model = settled()
        val missing = CompletableDeferred<Unit>()
        model.useMangaForMigration(current = 2L, target = 150L) { missing.complete(Unit) }
        runBlocking { withTimeout(WAIT_MS) { missing.await() } }
        model.items[1].searchResult.value shouldBe SearchResult.NotFound
    }

    @Test
    fun aManualMatchThatFails() {
        val model = settled()
        every { harness.sourceManager.get(3L) } returns null
        val missing = CompletableDeferred<Unit>()
        model.useMangaForMigration(current = 2L, target = 3L) { missing.complete(Unit) }
        runBlocking { withTimeout(WAIT_MS) { missing.await() } }
        model.items[1].searchResult.value shouldBe SearchResult.NotFound
    }
}
