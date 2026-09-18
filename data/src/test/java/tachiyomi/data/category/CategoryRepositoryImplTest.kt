package tachiyomi.data.category

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.data.inMemoryDatabase
import tachiyomi.data.seedManga
import tachiyomi.data.seedMangaCategory
import tachiyomi.domain.category.model.Category

/** The read side; the writes are in [CategoryRepositoryWriteTest]. */
internal class CategoryRepositoryImplTest {
    private val database = inMemoryDatabase()
    private val repository = CategoryRepositoryImpl(database)
    private var mangaId = 0L
    private var categoryId = 0L

    @BeforeEach
    fun seed() = runTest {
        // One user category holding one manga, next to the seeded system category (id 0, name "").
        categoryId = repository.insert(
            Category(id = 0L, name = "Read", order = 1L, flags = 2L, version = 3L, uid = 4L, lastModifiedAt = 5L),
        )!!
        mangaId = database.seedManga(title = "A")
        database.seedMangaCategory(mangaId, categoryId)
    }

    @Test
    fun getReturnsMappedCategory() = runTest {
        repository.get(categoryId) shouldBe Category(
            id = categoryId,
            name = "Read",
            order = 1L,
            flags = 2L,
            version = 3L,
            uid = 4L,
            lastModifiedAt = 5L,
        )
        repository.get(0L)?.isSystemCategory shouldBe true
        repository.get(99L) shouldBe null
    }

    @Test
    fun getAllListsInsertedCategory() = runTest {
        repository.getAll().map { it.name } shouldBe listOf("", "Read")
        repository.getAllAsFlow().first().map { it.name } shouldBe listOf("", "Read")
    }

    @Test
    fun categoriesByMangaId() = runTest {
        repository.getCategoriesByMangaId(mangaId).map { it.id } shouldBe listOf(categoryId)
        repository.getCategoriesByMangaId(mangaId + 1L) shouldBe emptyList()
    }

    @Test
    fun categoriesByMangaIdFlow() = runTest {
        repository.getCategoriesByMangaIdAsFlow(mangaId).first().map { it.name } shouldBe listOf("Read")
    }
}
