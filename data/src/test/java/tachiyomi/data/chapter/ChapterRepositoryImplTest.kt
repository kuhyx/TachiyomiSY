package tachiyomi.data.chapter

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.data.inMemoryDatabase
import tachiyomi.data.seedManga
import tachiyomi.domain.chapter.model.Chapter

internal class ChapterRepositoryImplTest {
    private val database = inMemoryDatabase()
    private val repository = ChapterRepositoryImpl(database)

    @Test
    fun composesTheThreeFacets() = runTest {
        val mangaId = database.seedManga(title = "A")
        val added = repository.addAll(listOf(Chapter.create().copy(mangaId = mangaId, url = "/c/1", name = "1")))
        repository.getChapterById(added.single().id)?.name shouldBe "1"
        repository.getScanlatorsByMergeId(mangaId) shouldBe emptyList()
    }
}
