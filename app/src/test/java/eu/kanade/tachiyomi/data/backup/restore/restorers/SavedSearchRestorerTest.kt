package eu.kanade.tachiyomi.data.backup.restore.restorers

import eu.kanade.tachiyomi.data.backup.BackupKoin
import eu.kanade.tachiyomi.data.backup.fakeQuery
import eu.kanade.tachiyomi.data.backup.models.BackupSavedSearch
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.data.SelectNamesAndSources

internal class SavedSearchRestorerTest {

    private val graph = BackupKoin()
    private val restorer = SavedSearchRestorer(graph.database)

    @Test
    fun nothingToRestore() = runTest {
        restorer.restoreSavedSearches(emptyList())
        verify(exactly = 0) { graph.savedSearches.selectNamesAndSources() }
    }

    @Test
    fun onlyNewSearchesAreInserted() = runTest {
        every { graph.savedSearches.selectNamesAndSources() } returns fakeQuery(
            listOf(SelectNamesAndSources(source = 1L, name = "Kept")),
        )
        restorer.restoreSavedSearches(
            listOf(
                BackupSavedSearch(name = "Kept", source = 1L),
                BackupSavedSearch(name = "Kept", query = "q", filterList = "[{}]", source = 2L),
                BackupSavedSearch(name = "Other", query = " ", filterList = "[]", source = 1L),
                BackupSavedSearch(name = "Blank", source = 3L),
            ),
        )
        coVerify(exactly = 0) {
            graph.savedSearches.insert(source = 1L, name = "Kept", query = any(), filtersJson = any())
        }
        coVerify { graph.savedSearches.insert(source = 2L, name = "Kept", query = "q", filtersJson = "[{}]") }
        coVerify { graph.savedSearches.insert(source = 1L, name = "Other", query = null, filtersJson = null) }
        coVerify { graph.savedSearches.insert(source = 3L, name = "Blank", query = null, filtersJson = null) }
    }
}
