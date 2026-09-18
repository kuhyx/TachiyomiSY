package tachiyomi.data.manga

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.data.inMemoryDatabase
import tachiyomi.domain.manga.model.MangaUpdate

/** The composite forwards each facet to its own implementation. */
internal class MangaRepositoryImplTest {
    private val database = inMemoryDatabase()
    private val repository = MangaRepositoryImpl(database)

    @Test
    fun facetsShareOneDatabase() = runTest {
        val id = database.insertManga(url = "/a", title = "Old")

        repository.update(MangaUpdate(id = id, title = "New")) shouldBe true

        repository.getMangaById(id).ogTitle shouldBe "New"
        repository.getReadMangaNotInLibrary() shouldBe emptyList()
        repository.getMangaBySourceId(1L).map { it.id } shouldBe listOf(id)
    }
}
