package tachiyomi.data.chapter

import exh.source.MERGED_SOURCE_ID
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.data.inMemoryDatabase
import tachiyomi.data.seedChapter
import tachiyomi.data.seedExcluded
import tachiyomi.data.seedManga
import tachiyomi.data.seedMerged

internal class ChapterMergedRepositoryImplTest {
    private val database = inMemoryDatabase()
    private val repository = ChapterMergedRepositoryImpl(database)
    private var mergedId = 0L

    @BeforeEach
    fun seed() = runTest {
        // A merged manga with one child holding chapters without, with a kept and with an excluded scanlator.
        mergedId = database.seedManga(title = "Merged", source = MERGED_SOURCE_ID)
        val child = database.seedManga(title = "Child", source = 2L, favorite = false)
        database.seedMerged(mergeId = mergedId, mangaId = child)
        database.seedChapter(mangaId = child, name = "1")
        database.seedChapter(mangaId = child, name = "2", scanlator = "Sub")
        database.seedChapter(mangaId = child, name = "3", scanlator = "Bad")
        database.seedExcluded(mergedId, "Bad")
    }

    @Test
    fun mergedChaptersHonourFilter() = runTest {
        val all = repository.getMergedChapterByMangaId(mergedId, applyScanlatorFilter = false)
        all.map { it.name }.sorted() shouldBe listOf("1", "2", "3")
        val kept = repository.getMergedChapterByMangaId(mergedId, applyScanlatorFilter = true)
        kept.map { it.name }.sorted() shouldBe listOf("1", "2")
        repository.getMergedChapterByMangaId(999L, applyScanlatorFilter = false) shouldBe emptyList()
    }

    @Test
    fun mergedChaptersFlow() = runTest {
        val all = repository.getMergedChapterByMangaIdFlow(mergedId, applyScanlatorFilter = false).first()
        all.map { it.name }.sorted() shouldBe listOf("1", "2", "3")
        val kept = repository.getMergedChapterByMangaIdFlow(mergedId, applyScanlatorFilter = true).first()
        kept.map { it.name }.sorted() shouldBe listOf("1", "2")
    }

    @Test
    fun scanlatorsByMergeId() = runTest {
        repository.getScanlatorsByMergeId(mergedId).sorted() shouldBe listOf("", "Bad", "Sub")
        repository.getScanlatorsByMergeId(999L) shouldBe emptyList()
    }

    @Test
    fun scanlatorsByMergeIdFlow() = runTest {
        repository.getScanlatorsByMergeIdAsFlow(mergedId).first().sorted() shouldBe listOf("", "Bad", "Sub")
    }
}
