package tachiyomi.data.chapter

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.data.ChaptersQueries
import tachiyomi.data.Database
import tachiyomi.data.inMemoryDatabase
import tachiyomi.data.seedChapter
import tachiyomi.data.seedManga
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.model.ChapterUpdate

internal class ChapterWriteRepositoryImplTest {
    private val database = inMemoryDatabase()
    private val repository = ChapterWriteRepositoryImpl(database)
    private val queries = ChapterQueryRepositoryImpl(database)
    private var mangaId = 0L

    @BeforeEach
    fun seed() = runTest {
        mangaId = database.seedManga(title = "A")
    }

    private fun chapterOf(name: String, scanlator: String? = null): Chapter = Chapter.create().copy(
        mangaId = mangaId,
        url = "/c/$name",
        name = name,
        scanlator = scanlator,
        dateUpload = 3L,
    )

    @Test
    fun addAllReturnsChaptersWithIds() = runTest {
        val added = repository.addAll(listOf(chapterOf("1"), chapterOf("2", scanlator = "Grp")))
        added.map { it.name } shouldBe listOf("1", "2")
        added.map { it.id }.toSet().size shouldBe 2
        added.all { it.id > 0L } shouldBe true
        queries.getChapterById(added[1].id) shouldBe added[1]
    }

    @Test
    fun addAllOfNothingIsEmpty() = runTest {
        repository.addAll(emptyList()) shouldBe emptyList()
    }

    @Test
    fun addAllFailureIsEmpty() = runTest {
        val orphan = chapterOf("1").copy(mangaId = 999L)
        repository.addAll(listOf(orphan)) shouldBe emptyList()
        queries.getChapterByUrl("/c/1") shouldBe emptyList()
    }

    @Test
    fun updateChangesGivenColumns() = runTest {
        val id = database.seedChapter(mangaId = mangaId, name = "1", scanlator = "Grp")
        val memo = JsonObject(mapOf("k" to JsonPrimitive("v")))
        repository.update(ChapterUpdate(id = id, read = true, lastPageRead = 5L, memo = memo))
        val chapter = queries.getChapterById(id)
        chapter?.read shouldBe true
        chapter?.lastPageRead shouldBe 5L
        chapter?.memo shouldBe memo
        chapter?.scanlator shouldBe "Grp"
        chapter?.name shouldBe "1"
        chapter?.version shouldNotBe 0L
    }

    @Test
    fun updateAllAppliesEachUpdate() = runTest {
        val first = database.seedChapter(mangaId = mangaId, name = "1")
        val second = database.seedChapter(mangaId = mangaId, name = "2")
        repository.updateAll(
            listOf(
                ChapterUpdate(id = first, name = "one", url = "/one", bookmark = true, chapterNumber = 1.0),
                ChapterUpdate(id = second, name = "two", scanlator = "S", sourceOrder = 7L, dateFetch = 8L),
            ),
        )
        repository.updateAll(emptyList())
        queries.getChapterById(first)?.name shouldBe "one"
        queries.getChapterById(first)?.bookmark shouldBe true
        queries.getChapterById(second)?.scanlator shouldBe "S"
        queries.getChapterById(second)?.sourceOrder shouldBe 7L
    }

    @Test
    fun removeChaptersWithIds() = runTest {
        val first = database.seedChapter(mangaId = mangaId, name = "1")
        val second = database.seedChapter(mangaId = mangaId, name = "2")
        repository.removeChaptersWithIds(listOf(first))
        queries.getChapterByMangaId(mangaId, applyScanlatorFilter = false).map { it.id } shouldBe listOf(second)
    }

    @Test
    fun removeFailureIsSwallowed() = runTest {
        val failingQueries = mockk<ChaptersQueries> {
            coEvery { removeChaptersWithIds(any()) } throws IllegalStateException("boom")
        }
        val failing = mockk<Database> { every { chaptersQueries } returns failingQueries }
        ChapterWriteRepositoryImpl(failing).removeChaptersWithIds(listOf(1L))
    }
}
