package eu.kanade.tachiyomi.data.backup.restore.restorers

import eu.kanade.tachiyomi.data.backup.BackupKoin
import eu.kanade.tachiyomi.data.backup.fakeQuery
import eu.kanade.tachiyomi.data.backup.models.BackupCategory
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.domain.category.model.Category

internal class CategoriesRestorerTest {

    private val graph = BackupKoin()
    private val restorer = CategoriesRestorer(graph.database, graph.getCategories, graph.libraryPreferences)

    @BeforeEach
    fun setUp() {
        every { graph.categories.insert(any(), any(), any(), any(), any(), any()) } returns fakeQuery(listOf(40L))
    }

    private fun verifyUpdate(categoryId: Long, uid: Long) = coVerify {
        graph.categories.update(
            name = any(),
            order = any(),
            flags = any(),
            version = any(),
            uid = uid,
            last_modified_at = any(),
            isSyncing = 1L,
            categoryId = categoryId,
        )
    }

    @Test
    fun nothingToRestore() = runTest {
        restorer(emptyList())
        coVerify(exactly = 0) { graph.getCategories.await() }
    }

    @Test
    fun newCategoriesAreAppended() = runTest {
        coEvery { graph.getCategories.await() } returns listOf(
            Category(id = 1L, name = "Local", order = 4L, flags = 0L),
            Category(id = 2L, name = "Earlier", order = 1L, flags = 0L),
        )
        restorer(listOf(BackupCategory(name = "B", order = 2, flags = 8), BackupCategory(name = "A", order = 1)))
        verify { graph.categories.insert("A", 5L, 0L, 0L, 0L, 0L) }
        verify { graph.categories.insert("B", 6L, 8L, 0L, 0L, 0L) }
        coVerify { graph.categories.resetIsSyncing() }
        graph.libraryPreferences.categorizedDisplaySettings.get() shouldBe true
    }

    @Test
    fun firstCategoryStartsAtZero() = runTest {
        restorer(listOf(BackupCategory(name = "Only")))
        verify { graph.categories.insert("Only", 0L, 0L, 0L, 0L, 0L) }
        graph.libraryPreferences.categorizedDisplaySettings.get() shouldBe false
    }

    @Test
    fun existingCategoriesAreUpdated() = runTest {
        coEvery { graph.getCategories.await() } returns listOf(
            Category(id = 1L, name = "ByUid", order = 0L, flags = 0L, uid = 11L),
            Category(id = 2L, name = "ByName", order = 1L, flags = 0L, uid = 22L),
            Category(id = 3L, name = "Legacy", order = 2L, flags = 0L, uid = 33L),
        )
        restorer(
            listOf(
                BackupCategory(name = "Renamed", order = 0, uid = 11L),
                BackupCategory(name = "ByName", order = 1, uid = 99L),
                BackupCategory(name = "Legacy", order = 2),
            ),
        )
        verifyUpdate(categoryId = 1L, uid = 11L)
        verifyUpdate(categoryId = 2L, uid = 99L)
        verifyUpdate(categoryId = 3L, uid = 33L)
        verify(exactly = 0) { graph.categories.insert(any(), any(), any(), any(), any(), any()) }
    }
}
