package tachiyomi.data.chapter

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.data.inMemoryDatabase
import tachiyomi.data.seedChapter
import tachiyomi.data.seedExcluded
import tachiyomi.data.seedManga

internal class ChapterQueryRepositoryImplTest {
    private val database = inMemoryDatabase()
    private val repository = ChapterQueryRepositoryImpl(database)
    private var mangaId = 0L
    private var firstId = 0L
    private var bookmarkedId = 0L

    @BeforeEach
    fun seed() = runTest {
        // Three chapters: no scanlator, a kept scanlator and an excluded one; another manga shares a url.
        mangaId = database.seedManga(title = "A")
        firstId = database.seedChapter(mangaId = mangaId, name = "1", url = SHARED_URL)
        bookmarkedId = database.seedChapter(mangaId = mangaId, name = "2", scanlator = "Grp", bookmark = true)
        database.seedChapter(mangaId = mangaId, name = "3", scanlator = "Bad")
        database.seedExcluded(mangaId, "Bad")
        database.seedChapter(mangaId = database.seedManga(title = "B"), name = "1", url = SHARED_URL)
    }

    @Test
    fun chaptersByMangaIdFiltered() = runTest {
        val all = repository.getChapterByMangaId(mangaId, applyScanlatorFilter = false)
        all.map { it.name }.sorted() shouldBe listOf("1", "2", "3")
        val kept = repository.getChapterByMangaId(mangaId, applyScanlatorFilter = true)
        kept.map { it.name }.sorted() shouldBe listOf("1", "2")
    }

    @Test
    fun chaptersByMangaIdFlow() = runTest {
        val all = repository.getChapterByMangaIdAsFlow(mangaId, applyScanlatorFilter = false).first()
        all.map { it.name }.sorted() shouldBe listOf("1", "2", "3")
        val kept = repository.getChapterByMangaIdAsFlow(mangaId, applyScanlatorFilter = true).first()
        kept.map { it.name }.sorted() shouldBe listOf("1", "2")
    }

    @Test
    fun scanlatorsByMangaId() = runTest {
        repository.getScanlatorsByMangaId(mangaId).sorted() shouldBe listOf("", "Bad", "Grp")
        repository.getScanlatorsByMangaId(999L) shouldBe emptyList()
    }

    @Test
    fun scanlatorsByMangaIdFlow() = runTest {
        repository.getScanlatorsByMangaIdAsFlow(mangaId).first().sorted() shouldBe listOf("", "Bad", "Grp")
    }

    @Test
    fun bookmarkedChapters() = runTest {
        repository.getBookmarkedChaptersByMangaId(mangaId).map { it.id } shouldBe listOf(bookmarkedId)
    }

    @Test
    fun chapterById() = runTest {
        repository.getChapterById(firstId)?.name shouldBe "1"
        repository.getChapterById(999L) shouldBe null
    }

    @Test
    fun chapterByUrlAndMangaId() = runTest {
        repository.getChapterByUrlAndMangaId(SHARED_URL, mangaId)?.id shouldBe firstId
        repository.getChapterByUrlAndMangaId("/nope", mangaId) shouldBe null
    }

    @Test
    fun chaptersByUrlSpanManga() = runTest {
        repository.getChapterByUrl(SHARED_URL).map { it.mangaId }.sorted() shouldBe listOf(mangaId, mangaId + 1L)
        repository.getChapterByUrl("/nope") shouldBe emptyList()
    }

    private companion object {
        const val SHARED_URL = "/shared"
    }
}
