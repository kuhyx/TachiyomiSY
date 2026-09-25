package eu.kanade.tachiyomi.data.backup.create.creators

import eu.kanade.tachiyomi.data.backup.fakeQuery
import eu.kanade.tachiyomi.data.backup.models.BackupSavedSearch
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import tachiyomi.data.Database

internal class SavedSearchBackupCreatorTest {

    private val database = mockk<Database>()

    private fun savedSearches(rows: List<BackupSavedSearch>) {
        val queries = mockk<tachiyomi.data.Saved_searchQueries>()
        every { queries.selectAll(mapper = any<Function5<Long, Long, String, String?, String?, BackupSavedSearch>>()) }
            .returns(fakeQuery(rows))
        every { database.saved_searchQueries } returns queries
    }

    @BeforeEach
    fun setUp() {
        startKoin { modules(module { single { database } }) }
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun returnsEverySavedSearch() = runTest {
        savedSearches(listOf(BackupSavedSearch(name = "A"), BackupSavedSearch(name = "B")))
        val creator = SavedSearchBackupCreator(database = database)
        creator().map { it.name } shouldBe listOf("A", "B")
    }

    @Test
    fun injectsDatabaseByDefault() = runTest {
        savedSearches(listOf(BackupSavedSearch(name = "Injected")))
        SavedSearchBackupCreator()().single().name shouldBe "Injected"
    }

    @Test
    fun emptyWhenNoSavedSearches() = runTest {
        savedSearches(emptyList())
        SavedSearchBackupCreator(database = database)() shouldBe emptyList()
    }
}
