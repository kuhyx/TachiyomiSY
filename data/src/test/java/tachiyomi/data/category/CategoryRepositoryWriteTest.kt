package tachiyomi.data.category

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.data.inMemoryDatabase
import tachiyomi.domain.category.model.Category
import tachiyomi.domain.category.model.CategoryUpdate

/** The write side of [CategoryRepositoryImpl]. */
internal class CategoryRepositoryWriteTest {
    private val repository = CategoryRepositoryImpl(inMemoryDatabase())

    private suspend fun insert(name: String, order: Long): Long =
        repository.insert(Category(id = 0L, name = name, order = order, flags = 0L, uid = 9L, lastModifiedAt = 9L))!!

    @Test
    fun insertReturnsNewId() = runTest {
        val first = insert("A", order = 1L)
        val second = insert("B", order = 2L)
        second shouldNotBe first
        repository.get(second)?.name shouldBe "B"
    }

    @Test
    fun updatePartialChangesColumns() = runTest {
        val id = insert("A", order = 1L)
        repository.updatePartial(CategoryUpdate(id = id, name = "Renamed", flags = 4L))
        val updated = repository.get(id)
        updated?.name shouldBe "Renamed"
        updated?.flags shouldBe 4L
        updated?.order shouldBe 1L
        updated?.version shouldBe 1L
    }

    @Test
    fun updatePartialAppliesEach() = runTest {
        val first = insert("A", order = 1L)
        val second = insert("B", order = 2L)
        repository.updatePartial(
            listOf(
                CategoryUpdate(id = first, order = 5L, version = 7L, uid = 8L, lastModifiedAt = 9L),
                CategoryUpdate(id = second, name = "Second"),
            ),
        )
        repository.updatePartial(emptyList())
        repository.get(first)?.order shouldBe 5L
        repository.get(second)?.name shouldBe "Second"
    }

    @Test
    fun updateAllFlags() = runTest {
        val id = insert("A", order = 1L)
        repository.updateAllFlags(6L)
        repository.getAll().map { it.flags } shouldBe listOf(6L, 6L)
        repository.updateAllFlags(null)
        repository.get(id)?.flags shouldBe 6L
    }

    @Test
    fun deleteRemovesCategory() = runTest {
        val id = insert("A", order = 1L)
        repository.delete(id)
        repository.get(id) shouldBe null
        repository.getAll().size shouldBe 1
    }
}
