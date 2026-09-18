package tachiyomi.data.manga

import exh.source.MERGED_SOURCE_ID
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tachiyomi.data.Database
import tachiyomi.data.InjektHarness

/** Inserts a `merged` reference of [part] into [merge]; [download] is its `download_chapters` flag. */
internal suspend fun Database.insertReference(merge: Long, part: Long?, download: Boolean = true) {
    mergedQueries.insert(
        infoManga = false, getChapterUpdates = true, chapterSortMode = 1L, chapterPriority = 2L,
        downloadChapters = download, mergeId = merge, mergeUrl = "/merge/$merge", mangaId = part,
        mangaUrl = "/part/$part", mangaSource = 1L,
    )
}

internal class MangaMergeReadRepositoryImplTest {
    private val harness = InjektHarness()
    private val database = harness.database
    private val repository = MangaMergeReadRepositoryImpl(database)

    @BeforeEach
    fun installScope() {
        harness.install()
    }

    @AfterEach
    fun restoreScope() {
        harness.uninstall()
    }

    private suspend fun seedMerge(): Triple<Long, Long, Long> {
        val merge = database.insertManga(url = "/merge", source = MERGED_SOURCE_ID, favorite = true)
        val first = database.insertManga(url = "/a")
        val second = database.insertManga(url = "/b")
        database.insertReference(merge = merge, part = first)
        database.insertReference(merge = merge, part = second, download = false)
        database.insertReference(merge = merge, part = null)
        return Triple(merge, first, second)
    }

    @Test
    fun getMergedManga() = runTest {
        val (_, first, second) = seedMerge()
        database.insertManga(url = "/c")

        repository.getMergedManga().map { it.id } shouldBe listOf(first, second)
        repository.subscribeMergedManga().first().map { it.id } shouldBe listOf(first, second)
    }

    @Test
    fun getMergedMangaById() = runTest {
        val (merge, first, second) = seedMerge()
        val other = database.insertManga(url = "/other", source = MERGED_SOURCE_ID)
        database.insertReference(merge = other, part = first)

        repository.getMergedMangaById(merge).map { it.id } shouldBe listOf(first, second)
        repository.subscribeMergedMangaById(other).first().map { it.id } shouldBe listOf(first)
    }

    @Test
    fun getReferencesById() = runTest {
        val (merge, first, second) = seedMerge()

        val references = repository.getReferencesById(merge)

        references.map { it.mangaId } shouldBe listOf(first, second, null)
        references.first().mergeUrl shouldBe "/merge/$merge"
        references.first().chapterPriority shouldBe 2
        repository.subscribeReferencesById(merge).first().size shouldBe 3
        repository.getReferencesById(merge + 100L) shouldBe emptyList()
    }

    @Test
    fun getMergeMangaForDownloading() = runTest {
        val (merge, first, _) = seedMerge()

        repository.getMergeMangaForDownloading(merge).map { it.id } shouldBe listOf(first)
    }
}
